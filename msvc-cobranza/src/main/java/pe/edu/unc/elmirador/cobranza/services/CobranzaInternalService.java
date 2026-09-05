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
import pe.edu.unc.elmirador.cobranza.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.cobranza.models.entity.CuentaCorrienteDelCliente;
import pe.edu.unc.elmirador.cobranza.models.entity.CuentaPorCobrar;
import pe.edu.unc.elmirador.cobranza.models.entity.PeticionIdempotente;
import pe.edu.unc.elmirador.cobranza.models.vo.Dinero;
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

        CuentaCorrienteDelCliente cuentaCorriente = repositorio.findByClienteId(peticion.clienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("cliente", peticion.clienteId()));

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

    @Transactional(readOnly = true)
    public EstadoCrediticioResponse estadoCrediticio(String clienteId) {
        CuentaCorrienteDelCliente cuenta = repositorio.findByClienteId(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("cliente", clienteId));

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
