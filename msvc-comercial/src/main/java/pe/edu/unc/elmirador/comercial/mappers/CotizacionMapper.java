package pe.edu.unc.elmirador.comercial.mappers;

import pe.edu.unc.elmirador.comercial.dto.response.CotizacionResponse;
import pe.edu.unc.elmirador.comercial.dto.response.TarifaResponse;
import pe.edu.unc.elmirador.comercial.models.entity.Cotizacion;

public final class CotizacionMapper {

    private CotizacionMapper() {
    }

    public static CotizacionResponse aRespuesta(Cotizacion cotizacion) {
        return new CotizacionResponse(
                cotizacion.id(),
                cotizacion.clienteId(),
                cotizacion.tarifarioId(),
                cotizacion.carga().pesoKg(),
                cotizacion.carga().volumenM3(),
                cotizacion.carga().tipo().name(),
                cotizacion.ruta().origen(),
                cotizacion.ruta().destino(),
                cotizacion.ruta().corredor(),
                new TarifaResponse(
                        cotizacion.tarifa().base().monto(),
                        cotizacion.tarifa().base().codigoMoneda(),
                        cotizacion.tarifa().recargos().stream().map(r -> new TarifaResponse.RecargoResponse(
                                r.tipo().name(),
                                r.porcentaje()
                        )).toList(),
                        cotizacion.tarifa().descuento() != null ? new TarifaResponse.DescuentoResponse(
                                cotizacion.tarifa().descuento().porcentaje(),
                                cotizacion.tarifa().descuento().autorizadoPor()
                        ) : null
                ),
                cotizacion.vigencia().desde(),
                cotizacion.vigencia().hasta(),
                cotizacion.estado().name(),
                cotizacion.motivoDeRechazo() != null ? cotizacion.motivoDeRechazo().name() : null
        );
    }
}
