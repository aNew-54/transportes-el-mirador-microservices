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
import pe.edu.unc.elmirador.cobranza.dto.internal.request.ImporteRequest;
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

        if (!"CREDITO".equalsIgnoreCase(peticion.condicionDePago().modalidad())) {
            String pseudoId = "CONTADO-" + UUID.randomUUID().toString();
            idempotencia.save(new PeticionIdempotente(clave, pseudoId, OffsetDateTime.now(reloj)));
            return new ResultadoIdempotente<>(new CuentaPorCobrarCreadaResponse(peticion.facturaId(), pseudoId), false);
        }

        CuentaCorrienteDelCliente cuentaCorriente = repositorio.findByClienteId(peticion.clienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("cliente", peticion.clienteId()));

        String nuevaCuentaId = UUID.randomUUID().toString();
        CuentaPorCobrar cuenta = new CuentaPorCobrar(
                nuevaCuentaId,
                peticion.clienteId(),
                peticion.facturaId(),
                peticion.documentoId(),
                total,
                detraccion,
                montoNeto,
                peticion.fechaDeVencimiento().toLocalDate()
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

        List<ImporteRequest> deudaPorMoneda = new ArrayList<>();
        for (String moneda : cuenta.monedasConDeuda()) {
            Dinero deuda = cuenta.deudaTotal(moneda);
            deudaPorMoneda.add(new ImporteRequest(deuda.monto().toString(), moneda));
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
