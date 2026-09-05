package pe.edu.unc.elmirador.comercial.dto.response;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

public record ContratoMarcoResponse(
        String id,
        String clienteId,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        int tiempoLibreHoras,
        boolean consolidacionPermitida,
        List<String> consolidacionRestricciones,
        List<TarifaPactadaResponse> tarifasPactadas
) {
    public record TarifaPactadaResponse(
            String id,
            String rutaOrigen,
            String rutaDestino,
            String rutaCorredor,
            String tipoUnidad,
            BigDecimal precioMonto,
            String precioMoneda
    ) {
    }
}
