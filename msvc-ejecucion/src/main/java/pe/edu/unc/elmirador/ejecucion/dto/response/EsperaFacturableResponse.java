package pe.edu.unc.elmirador.ejecucion.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record EsperaFacturableResponse(
        OffsetDateTime inicio,
        OffsetDateTime fin,
        Integer tiempoLibreHoras,
        BigDecimal excedente
) {
}
