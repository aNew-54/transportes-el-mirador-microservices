package pe.edu.unc.elmirador.comercial.mappers;

import pe.edu.unc.elmirador.comercial.dto.response.TarifarioResponse;
import pe.edu.unc.elmirador.comercial.models.entity.Tarifario;

public final class TarifarioMapper {

    private TarifarioMapper() {
    }

    public static TarifarioResponse aRespuesta(Tarifario tarifario) {
        return new TarifarioResponse(
                tarifario.id(),
                tarifario.vigencia().desde(),
                tarifario.vigencia().hasta(),
                tarifario.precios().stream().map(p -> new TarifarioResponse.PrecioDeTarifarioResponse(
                        p.id(),
                        p.ruta().origen(),
                        p.ruta().destino(),
                        p.ruta().corredor(),
                        p.tipoDeUnidad().name(),
                        p.precio().monto(),
                        p.precio().codigoMoneda()
                )).toList(),
                tarifario.recargosEstandar().stream().map(r -> new TarifarioResponse.RecargoResponse(
                        r.tipo().name(),
                        r.porcentaje()
                )).toList()
        );
    }
}
