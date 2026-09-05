package pe.edu.unc.elmirador.comercial.mappers;

import pe.edu.unc.elmirador.comercial.dto.response.CondicionDePagoResponse;
import pe.edu.unc.elmirador.comercial.dto.response.OrdenDeServicioResponse;
import pe.edu.unc.elmirador.comercial.dto.response.TarifaResponse;
import pe.edu.unc.elmirador.comercial.models.entity.OrdenDeServicio;
import pe.edu.unc.elmirador.comercial.models.vo.Tarifa;

public final class OrdenDeServicioMapper {

    private OrdenDeServicioMapper() {
    }

    public static OrdenDeServicioResponse aRespuesta(OrdenDeServicio orden) {
        return new OrdenDeServicioResponse(
                orden.id(),
                orden.clienteId(),
                orden.contratoId(),
                orden.carga().pesoKg(),
                orden.carga().volumenM3(),
                orden.carga().tipo().name(),
                orden.ruta().origen(),
                orden.ruta().destino(),
                orden.ruta().corredor(),
                mapearTarifa(orden.tarifa()),
                new CondicionDePagoResponse(
                        orden.condicionDePago().modalidad().name(),
                        orden.condicionDePago().plazoEnDias()
                ),
                orden.estado().name(),
                orden.falsoFlete() != null ? mapearTarifa(orden.falsoFlete()) : null,
                orden.canceladoPor()
        );
    }

    private static TarifaResponse mapearTarifa(Tarifa tarifa) {
        if (tarifa == null) {
            return null;
        }
        return new TarifaResponse(
                tarifa.base().monto(),
                tarifa.base().codigoMoneda(),
                tarifa.recargos() != null ? tarifa.recargos().stream().map(r -> new TarifaResponse.RecargoResponse(
                        r.tipo().name(),
                        r.porcentaje()
                )).toList() : java.util.List.of(),
                tarifa.descuento() != null ? new TarifaResponse.DescuentoResponse(
                        tarifa.descuento().porcentaje(),
                        tarifa.descuento().autorizadoPor()
                ) : null
        );
    }
}
