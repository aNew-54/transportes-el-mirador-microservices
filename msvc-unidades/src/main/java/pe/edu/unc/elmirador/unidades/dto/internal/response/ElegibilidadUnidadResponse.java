package pe.edu.unc.elmirador.unidades.dto.internal.response;

import java.math.BigDecimal;
import java.util.List;

public record ElegibilidadUnidadResponse(
        String unidadId,
        boolean elegible,
        List<String> motivos,
        CapacidadDto capacidad,
        String tipoUnidad,
        String estadoOperativo
) {
    public record CapacidadDto(int pesoMaximoKg, BigDecimal volumenMaximoM3) {
    }
}
