package pe.edu.unc.elmirador.programacion.dto.response;

import java.math.BigDecimal;
import pe.edu.unc.elmirador.programacion.models.vo.TipoDeCarga;

public record CargaResponse(
        String ordenDeServicioId,
        Integer pesoKg,
        BigDecimal volumenM3,
        TipoDeCarga tipo,
        Integer secuenciaDeDescarga
) {}
