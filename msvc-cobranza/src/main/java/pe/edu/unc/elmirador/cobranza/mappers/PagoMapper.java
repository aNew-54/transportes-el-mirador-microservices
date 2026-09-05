package pe.edu.unc.elmirador.cobranza.mappers;

import java.util.List;

import pe.edu.unc.elmirador.cobranza.dto.response.AplicacionDePagoResponse;
import pe.edu.unc.elmirador.cobranza.dto.response.PagoResponse;
import pe.edu.unc.elmirador.cobranza.models.entity.AplicacionDePago;
import pe.edu.unc.elmirador.cobranza.models.entity.Pago;

public final class PagoMapper {

    private PagoMapper() {
    }

    public static PagoResponse aRespuesta(Pago pago) {
        return new PagoResponse(
                pago.id(),
                pago.clienteId(),
                pago.monto().monto(),
                pago.monto().codigoMoneda(),
                pago.medioDePago().modalidad(),
                pago.medioDePago().referencia(),
                pago.fecha(),
                pago.montoAplicado().monto(),
                pago.montoAplicado().codigoMoneda(),
                pago.saldoSinAplicar().monto(),
                pago.saldoSinAplicar().codigoMoneda(),
                pago.aplicaciones().stream().map(PagoMapper::aRespuesta).toList()
        );
    }

    public static AplicacionDePagoResponse aRespuesta(AplicacionDePago aplicacion) {
        return new AplicacionDePagoResponse(
                aplicacion.id(),
                aplicacion.cuentaPorCobrarId(),
                aplicacion.importe().monto(),
                aplicacion.importe().codigoMoneda()
        );
    }
}
