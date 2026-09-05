package pe.edu.unc.elmirador.comercial.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;

public record TarifarioResponse(
        String id,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        List<PrecioDeTarifarioResponse> precios,
        List<RecargoResponse> recargosEstandar
) {
    public record PrecioDeTarifarioResponse(
            String id,
            String origen,
            String destino,
            String corredor,
            String tipoUnidad,
            BigDecimal precioMonto,
            String precioMoneda
    ) {
    }

    public record RecargoResponse(
            String tipo,
            BigDecimal porcentaje
    ) {
    }
}
