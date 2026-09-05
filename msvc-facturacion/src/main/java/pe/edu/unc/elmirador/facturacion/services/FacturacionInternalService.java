package pe.edu.unc.elmirador.facturacion.services;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unc.elmirador.facturacion.dto.internal.request.ConceptoFacturableRequest;
import pe.edu.unc.elmirador.facturacion.dto.internal.request.RegistrarConformidadRequest;
import pe.edu.unc.elmirador.facturacion.dto.internal.response.ConformidadRegistradaResponse;
import pe.edu.unc.elmirador.facturacion.dto.response.ResultadoIdempotente;
import pe.edu.unc.elmirador.facturacion.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.facturacion.models.entity.Factura;
import pe.edu.unc.elmirador.facturacion.models.entity.LineaDeFactura;
import pe.edu.unc.elmirador.facturacion.models.entity.PeticionIdempotente;
import pe.edu.unc.elmirador.facturacion.models.vo.ConceptoFacturable;
import pe.edu.unc.elmirador.facturacion.models.vo.Conformidad;
import pe.edu.unc.elmirador.facturacion.models.vo.Dinero;
import pe.edu.unc.elmirador.facturacion.repositories.FacturaRepository;
import pe.edu.unc.elmirador.facturacion.repositories.PeticionIdempotenteRepository;

/**
 * Lado proveedor del contrato 8.
 */
@Service
public class FacturacionInternalService {

    private final FacturaRepository repositorio;
    private final PeticionIdempotenteRepository idempotencia;
    private final Clock reloj;

    public FacturacionInternalService(
            FacturaRepository repositorio,
            PeticionIdempotenteRepository idempotencia,
            Clock reloj
    ) {
        this.repositorio = repositorio;
        this.idempotencia = idempotencia;
        this.reloj = reloj;
    }

    /**
     * Contrato 8 · conformidad. Idempotente por {@code Idempotency-Key} (regla 6).
     *
     * <p>El reintento se resuelve ANTES de tocar el agregado, y el registro de la clave va en la misma
     * transaccion que el efecto.
     */
    @Transactional
    public ResultadoIdempotente<ConformidadRegistradaResponse> registrarConformidad(
            String clave, RegistrarConformidadRequest peticion) {

        Optional<PeticionIdempotente> yaVista = idempotencia.findById(clave);
        if (yaVista.isPresent()) {
            return new ResultadoIdempotente<>(
                    new ConformidadRegistradaResponse(yaVista.get().getRecursoId(), peticion.ordenDeServicioId()), true);
        }

        Factura factura = repositorio.findByOrdenDeServicioId(peticion.ordenDeServicioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("factura para orden de servicio", peticion.ordenDeServicioId()));

        // Que una conformidad rechazada no cuente vive en EstadoDeConformidad, no aqui. Y la fecha
        // es la de la firma, que es el hecho que el contrato transmite: sustituirla por la del reloj
        // perderia el dato y fecharia la conformidad cuando se recibio el mensaje, no cuando ocurrio.
        Conformidad conformidad = new Conformidad(
                peticion.estado().cuentaComoRegistrada(),
                peticion.incidenciasSinResolver(),
                peticion.fechaDeFirma());
        factura.registrarConformidad(conformidad);

        for (ConceptoFacturableRequest cr : peticion.conceptosFacturables()) {
            LineaDeFactura linea = new LineaDeFactura(
                    UUID.randomUUID().toString(),
                    peticion.ordenDeServicioId(),
                    cr.concepto(),
                    cr.detalle(),
                    new Dinero(cr.monto(), cr.moneda())
            );
            factura.agregarLinea(linea);
        }

        repositorio.save(factura);
        idempotencia.save(new PeticionIdempotente(clave, factura.id(), OffsetDateTime.now(reloj)));

        return new ResultadoIdempotente<>(
                new ConformidadRegistradaResponse(factura.id(), peticion.ordenDeServicioId()), false);
    }
}
