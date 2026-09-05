package pe.edu.unc.elmirador.programacion.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CargaConsolidadaResponse(
        List<CargaResponse> cargas,
        Integer pesoTotalKg,
        BigDecimal volumenTotalM3
) {}
