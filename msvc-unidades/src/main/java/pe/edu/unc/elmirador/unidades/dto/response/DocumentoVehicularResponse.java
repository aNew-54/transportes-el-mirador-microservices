package pe.edu.unc.elmirador.unidades.dto.response;

import java.time.LocalDate;

import pe.edu.unc.elmirador.unidades.models.vo.TipoDeDocumento;

public record DocumentoVehicularResponse(
        String id,
        TipoDeDocumento tipo,
        LocalDate desde,
        LocalDate hasta,
        String numero
) {
}
