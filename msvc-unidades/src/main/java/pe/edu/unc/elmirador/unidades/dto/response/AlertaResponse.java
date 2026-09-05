package pe.edu.unc.elmirador.unidades.dto.response;

public record AlertaResponse(
        String unidadId,
        String placa,
        TipoDeAlerta tipo,
        String referencia,
        String detalle
) {
    public enum TipoDeAlerta {
        DOCUMENTO_POR_VENCER,
        MANTENIMIENTO_PROXIMO
    }
}
