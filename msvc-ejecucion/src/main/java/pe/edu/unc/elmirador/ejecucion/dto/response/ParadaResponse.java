package pe.edu.unc.elmirador.ejecucion.dto.response;

import pe.edu.unc.elmirador.ejecucion.models.vo.EstadoDeParada;

public record ParadaResponse(
        Integer secuencia,
        String ordenDeServicioId,
        String direccion,
        EstadoDeParada estado,
        ConformidadResponse conformidad,
        EsperaFacturableResponse espera
) {
}
