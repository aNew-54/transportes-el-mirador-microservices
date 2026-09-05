package pe.edu.unc.elmirador.ejecucion.mappers;

import pe.edu.unc.elmirador.ejecucion.dto.response.ComprobanteResponse;
import pe.edu.unc.elmirador.ejecucion.dto.response.GastoDeRutaResponse;
import pe.edu.unc.elmirador.ejecucion.dto.response.LiquidacionDeViajeResponse;
import pe.edu.unc.elmirador.ejecucion.models.entity.GastoDeRuta;
import pe.edu.unc.elmirador.ejecucion.models.entity.LiquidacionDeViaje;
import pe.edu.unc.elmirador.ejecucion.models.vo.Comprobante;
import pe.edu.unc.elmirador.ejecucion.models.vo.Saldo;

public final class LiquidacionDeViajeMapper {

    private LiquidacionDeViajeMapper() {
    }

    public static LiquidacionDeViajeResponse mapear(LiquidacionDeViaje liquidacion) {
        Saldo saldo = liquidacion.saldo();
        
        return new LiquidacionDeViajeResponse(
                liquidacion.getViajeId(),
                liquidacion.getConductorId(),
                liquidacion.getAnticipo().monto(),
                liquidacion.getAnticipo().codigoMoneda(),
                liquidacion.getGastos().stream().map(LiquidacionDeViajeMapper::mapear).toList(),
                liquidacion.getEstado(),
                liquidacion.getFechaDeAprobacion(),
                saldo.importe().monto(),
                saldo.importe().codigoMoneda(),
                saldo.signo().name()
        );
    }

    private static GastoDeRutaResponse mapear(GastoDeRuta gasto) {
        return new GastoDeRutaResponse(
                gasto.getConcepto(),
                gasto.getImporte().monto(),
                gasto.getImporte().codigoMoneda(),
                mapear(gasto.getComprobante()),
                gasto.getDescripcion()
        );
    }

    private static ComprobanteResponse mapear(Comprobante comprobante) {
        if (comprobante == null) return null;
        return new ComprobanteResponse(
                comprobante.tipo(),
                comprobante.numero(),
                comprobante.fecha()
        );
    }
}
