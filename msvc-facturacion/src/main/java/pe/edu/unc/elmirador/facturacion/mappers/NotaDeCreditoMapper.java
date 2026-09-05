package pe.edu.unc.elmirador.facturacion.mappers;

import pe.edu.unc.elmirador.facturacion.dto.response.NotaDeCreditoResponse;
import pe.edu.unc.elmirador.facturacion.models.entity.NotaDeCredito;

public final class NotaDeCreditoMapper {

    private NotaDeCreditoMapper() {}

    public static NotaDeCreditoResponse aRespuesta(NotaDeCredito nota) {
        return new NotaDeCreditoResponse(
            nota.id(),
            nota.facturaId(),
            nota.motivo(),
            nota.monto().monto(),
            nota.monto().codigoMoneda(),
            nota.fechaDeEmision(),
            nota.motivoDetalle()
        );
    }
}
