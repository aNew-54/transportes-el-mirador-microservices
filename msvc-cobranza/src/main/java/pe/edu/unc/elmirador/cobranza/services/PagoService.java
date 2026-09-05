package pe.edu.unc.elmirador.cobranza.services;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unc.elmirador.cobranza.dto.request.AplicacionRequest;
import pe.edu.unc.elmirador.cobranza.dto.request.AplicarPagoRequest;
import pe.edu.unc.elmirador.cobranza.dto.request.RegistrarPagoRequest;
import pe.edu.unc.elmirador.cobranza.dto.response.PagoResponse;
import pe.edu.unc.elmirador.cobranza.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.cobranza.mappers.PagoMapper;
import pe.edu.unc.elmirador.cobranza.models.entity.CuentaCorrienteDelCliente;
import pe.edu.unc.elmirador.cobranza.models.entity.CuentaPorCobrar;
import pe.edu.unc.elmirador.cobranza.models.entity.Pago;
import pe.edu.unc.elmirador.cobranza.models.vo.Dinero;
import pe.edu.unc.elmirador.cobranza.models.vo.MedioDePago;
import pe.edu.unc.elmirador.cobranza.repositories.CuentaCorrienteDelClienteRepository;
import pe.edu.unc.elmirador.cobranza.repositories.PagoRepository;

/**
 * Servicio de aplicacion del agregado {@code Pago}.
 *
 * <p>No necesita reloj: la fecha del pago es un dato del hecho y llega en la peticion.
 */
@Service
public class PagoService {

    private final PagoRepository pagoRepositorio;
    private final CuentaCorrienteDelClienteRepository cuentaRepositorio;

    public PagoService(PagoRepository pagoRepositorio, CuentaCorrienteDelClienteRepository cuentaRepositorio) {
        this.pagoRepositorio = pagoRepositorio;
        this.cuentaRepositorio = cuentaRepositorio;
    }

    @Transactional
    public PagoResponse registrar(RegistrarPagoRequest peticion) {
        Pago pago = new Pago(
                UUID.randomUUID().toString(),
                peticion.clienteId(),
                new Dinero(peticion.montoMonto(), peticion.montoMoneda()),
                new MedioDePago(peticion.modalidad(), peticion.referencia()),
                peticion.fecha()
        );
        return PagoMapper.aRespuesta(pagoRepositorio.save(pago));
    }

    /**
     * Aplica el pago a una o varias cuentas.
     *
     * <p>Cada cuenta se busca por su propio id, no dentro del titular del pago. Buscarla dentro del
     * titular garantizaria que siempre fuese suya, y PAG-02 —«un pago no puede aplicarse a cuentas de
     * un cliente distinto»— dejaria de poder violarse por la API: una cuenta ajena daria {@code 404}
     * en vez del {@code 422} que le corresponde. Quien decide si la acepta es el agregado {@code Pago}.
     */
    @Transactional
    public PagoResponse aplicar(String id, AplicarPagoRequest peticion) {
        Pago pago = pagoRepositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("pago", id));

        Set<CuentaCorrienteDelCliente> titulares = new LinkedHashSet<>();

        for (AplicacionRequest aplicacion : peticion.aplicaciones()) {
            String cuentaId = aplicacion.cuentaPorCobrarId();
            CuentaCorrienteDelCliente titular = cuentaRepositorio.findByCuentasId(cuentaId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("cuenta por cobrar", cuentaId));

            CuentaPorCobrar cuenta = titular.cuentas().stream()
                    .filter(c -> c.id().equals(cuentaId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "El repositorio devolvio al titular de la cuenta " + cuentaId + " sin esa cuenta"));

            pago.aplicarACuentaPorCobrar(cuenta, new Dinero(aplicacion.importeMonto(), aplicacion.importeMoneda()));
            titulares.add(titular);
        }

        titulares.forEach(cuentaRepositorio::save);
        return PagoMapper.aRespuesta(pagoRepositorio.save(pago));
    }
}
