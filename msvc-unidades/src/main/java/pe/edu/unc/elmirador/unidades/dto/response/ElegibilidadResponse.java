package pe.edu.unc.elmirador.unidades.dto.response;

import java.util.List;

public record ElegibilidadResponse(
        boolean elegible,
        List<String> motivos
) {
}
