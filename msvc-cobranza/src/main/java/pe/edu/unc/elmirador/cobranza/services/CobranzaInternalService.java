package pe.edu.unc.elmirador.cobranza.services;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unc.elmirador.cobranza.dto.internal.request.CrearCuentaPorCobrarRequest;
import pe.edu.unc.elmirador.cobranza.dto.internal.response.ImporteResponse;
import pe.edu.unc.elmirador.cobranza.dto.internal.response.CuentaPorCobrarCreadaResponse;
import pe.edu.unc.elmirador.cobranza.dto.internal.response.EstadoCrediticioResponse;
import pe.edu.unc.elmirador.cobranza.dto.response.ResultadoIdempotente;
import pe.edu.unc.elmirador.cobranza.exceptions.ImportesInconsistentesException;
import pe.edu.unc.elmirador.cobranza.models.entity.CuentaCorrienteDelCliente;
import pe.edu.unc.elmirador.cobranza.models.entity.CuentaPorCobrar;
import pe.edu.unc.elmirador.cobranza.models.entity.PeticionIdempotente;
import pe.edu.unc.elmirador.cobranza.models.vo.Dinero;
import pe.edu.unc.elmirador.cobranza.models.vo.EstadoCrediticio;
import pe.edu.unc.elmirador.cobranza.repositories.CuentaCorrienteDelClienteRepository;
import pe.edu.unc.elmirador.cobranza.repositories.PeticionIdempotenteRepository;

@Service
public class CobranzaInternalService {

    private final CuentaCorrienteDelClienteRepository repositorio;
    private final PeticionIdempotenteRepository idempotencia;
    private final Clock reloj;

    public CobranzaInternalService(
            CuentaCorrienteDelClienteRepository repositorio,
            PeticionIdempotenteRepository idempotencia,
            Clock reloj) {
        this.repositorio = repositorio;
        this.idempotencia = idempotencia;
        this.reloj = reloj;
    }

    /**
     * La cuenta corriente del cliente, abriendola si Cobranza no sabia de el todavia.
     *
     * <p>Hasta este arreglo los dos contratos hacian {@code orElseThrow} y ningun camino de
     * produccion construia jamas una {@code CuentaCorrienteDelCliente}: solo la fabricaban a mano
     * seis ficheros de prueba. La raiz de agregado era inalcanzable desde fuera, asi que el
     * contrato 10 respondia 404 a toda factura y el contrato 11 a toda consulta de credito, y
     * ningun cliente podia pedir una orden a credito ni entrar al ledger. Las 109 pruebas de este
     * modulo seguian en verde porque todas parten de un estado que produccion no sabia alcanzar.
     *
     * <p>Se abre aqui y no en el consumidor a proposito. Si Comercial resolviera «sin cuenta, luego
     * vigente», se estaria inventando un veredicto crediticio, que es justo lo que el contrato 11
     * existe para impedir. La regla es de Cobranza porque el estado crediticio es suyo.
     */
    private CuentaCorrienteDelCliente cuentaDe(String clienteId) {
        return repositorio.findByClienteId(clienteId).orElseGet(() -> abrirCuenta(clienteId));
    }

    /**
     * Un cliente del que Cobranza no sabe nada no debe nada, y quien no debe nada tiene el credito
     * intacto: nace VIGENTE, sin deuda y sin cuentas vencidas.
     *
     * <p>La cuenta se persiste en vez de sintetizar la respuesta al vuelo porque
     * {@code fechaDeCambio} viaja en el contrato 11 y Comercial la guarda y la compara por dia.
     * Una fecha calculada en cada lectura seria hoy siempre, y eso es mentir sobre cuando cambio
     * la situacion del cliente. Persistida, la fecha es un hecho: el dia que Cobranza abrio la
     * cuenta. Cobranza no lleva registro de clientes, asi que no puede distinguir un cliente nuevo
     * de un identificador que no existe en ninguna parte; quien si lo sabe es Comercial, que lo
     * busco en su propio registro antes de preguntar.
     *
     * <p>Dos lecturas simultaneas del mismo cliente no duplican la cuenta: {@code cliente_id} es la
     * clave primaria de {@code cuentas_corrientes}, asi que la segunda insercion choca y su
     * reintento encuentra la fila.
     */
    private CuentaCorrienteDelCliente abrirCuenta(String clienteId) {
        return repositorio.save(new CuentaCorrienteDelCliente(
                clienteId,
                EstadoCrediticio.vigente(LocalDate.now(reloj))));
    }

    @Transactional
    public ResultadoIdempotente<CuentaPorCobrarCreadaResponse> crearCuentaPorCobrar(String clave, CrearCuentaPorCobrarRequest peticion) {
        Optional<PeticionIdempotente> yaVista = idempotencia.findById(clave);
        if (yaVista.isPresent()) {
            return new ResultadoIdempotente<>(
                    new CuentaPorCobrarCreadaResponse(peticion.facturaId(), yaVista.get().getRecursoId()), true);
        }

        Dinero total = Dinero.de(peticion.total().monto(), peticion.total().moneda());
        Dinero detraccion = Dinero.de(peticion.detraccion().monto(), peticion.detraccion().moneda());
        Dinero montoNeto = Dinero.de(peticion.montoNeto().monto(), peticion.montoNeto().moneda());

        Dinero suma = montoNeto.sumar(detraccion);
        if (suma.monto().compareTo(total.monto()) != 0) {
            throw new ImportesInconsistentesException(
                    "Monto neto + detraccion no iguala el total (viola FAC-04)"
            );
        }

        CuentaCorrienteDelCliente cuentaCorriente = cuentaDe(peticion.clienteId());

        // Contado o credito lo decide el agregado: una factura al contado nace ya cancelada. Aqui solo
        // se le pasa lo que el contrato manda. La version anterior devolvia un id inventado
        // ("CONTADO-" + UUID) sin crear nada, de modo que el 201 apuntaba a un recurso inexistente.
        String nuevaCuentaId = UUID.randomUUID().toString();
        CuentaPorCobrar cuenta = CuentaPorCobrar.registrar(
                nuevaCuentaId,
                peticion.clienteId(),
                peticion.facturaId(),
                peticion.documentoId(),
                total,
                detraccion,
                montoNeto,
                peticion.fechaDeVencimiento().toLocalDate(),
                peticion.condicionDePago().modalidad()
        );

        cuentaCorriente.registrarCuenta(cuenta);
        repositorio.save(cuentaCorriente);
        idempotencia.save(new PeticionIdempotente(clave, nuevaCuentaId, OffsetDateTime.now(reloj)));

        return new ResultadoIdempotente<>(new CuentaPorCobrarCreadaResponse(peticion.facturaId(), nuevaCuentaId), false);
    }

    @Transactional
    public EstadoCrediticioResponse estadoCrediticio(String clienteId) {
        CuentaCorrienteDelCliente cuenta = cuentaDe(clienteId);

        List<ImporteResponse> deudaPorMoneda = new ArrayList<>();
        for (String moneda : cuenta.monedasConDeuda()) {
            Dinero deuda = cuenta.deudaTotal(moneda);
            deudaPorMoneda.add(new ImporteResponse(deuda.monto().toPlainString(), moneda));
        }

        LocalDate hoy = LocalDate.now(reloj);

        return new EstadoCrediticioResponse(
                cuenta.clienteId(),
                cuenta.estado().situacion().name(),
                cuenta.estado().fechaDeCambio(),
                cuenta.diasDeAtrasoMaximo(hoy),
                cuenta.cuentasVencidas(hoy),
                deudaPorMoneda
        );
    }
}
