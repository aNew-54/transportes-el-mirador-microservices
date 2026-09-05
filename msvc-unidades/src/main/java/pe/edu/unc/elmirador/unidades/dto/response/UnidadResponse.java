package pe.edu.unc.elmirador.unidades.dto.response;

import java.math.BigDecimal;
import java.util.List;

import pe.edu.unc.elmirador.unidades.models.vo.IntervaloDeMantenimiento;
import pe.edu.unc.elmirador.unidades.models.vo.SituacionOperativa;
import pe.edu.unc.elmirador.unidades.models.vo.TipoDeUnidad;

public record UnidadResponse(
        String id,
        String placa,
        TipoDeUnidad tipo,
        int pesoMaximoKg,
        BigDecimal volumenMaximoM3,
        int kilometraje,
        SituacionOperativa situacion,
        String motivoEstado,
        int kmUltimoServicio,
        int kmProximoServicio,
        IntervaloDeMantenimiento intervaloMantenimiento,
        List<DocumentoVehicularResponse> documentos
) {
}
