package pe.edu.unc.elmirador.ejecucion.dto.response;

import java.math.BigDecimal;

import pe.edu.unc.elmirador.ejecucion.models.vo.ConceptoDeGasto;

public record GastoDeRutaResponse(
        ConceptoDeGasto concepto,
        BigDecimal importeMonto,
        String importeMoneda,
        ComprobanteResponse comprobante,
        String descripcion
) {
}
