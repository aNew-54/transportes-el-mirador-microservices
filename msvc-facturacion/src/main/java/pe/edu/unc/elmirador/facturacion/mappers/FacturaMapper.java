package pe.edu.unc.elmirador.facturacion.mappers;

import pe.edu.unc.elmirador.facturacion.dto.response.*;
import pe.edu.unc.elmirador.facturacion.models.entity.Factura;
import pe.edu.unc.elmirador.facturacion.models.entity.LineaDeFactura;
import pe.edu.unc.elmirador.facturacion.models.vo.Conformidad;
import pe.edu.unc.elmirador.facturacion.models.vo.Detraccion;
import pe.edu.unc.elmirador.facturacion.models.vo.Dinero;
import pe.edu.unc.elmirador.facturacion.models.vo.SnapshotComercial;

public final class FacturaMapper {

    private FacturaMapper() {}

    public static FacturaResponse aRespuesta(Factura factura) {
        return new FacturaResponse(
            factura.id(),
            factura.ordenDeServicioId(),
            factura.clienteId(),
            factura.numeroDeComprobante() != null ? factura.numeroDeComprobante().formateado() : null,
            aRespuesta(factura.snapshotComercial()),
            aRespuesta(factura.detraccion()),
            aRespuesta(factura.conformidad()),
            factura.estado(),
            factura.fechaDeEmision(),
            factura.esFalsoFlete(),
            factura.total().monto(),
            factura.total().codigoMoneda(),
            factura.montoNeto().monto(),
            factura.montoNeto().codigoMoneda(),
            factura.saldoAjustable().monto(),
            factura.saldoAjustable().codigoMoneda(),
            factura.lineas().stream().map(FacturaMapper::aRespuesta).toList(),
            factura.ajustesAplicados().stream().map(FacturaMapper::aRespuesta).toList()
        );
    }

    private static SnapshotComercialResponse aRespuesta(SnapshotComercial snap) {
        return new SnapshotComercialResponse(
            snap.ordenDeServicioId(),
            snap.clienteId(),
            snap.tarifa().monto(),
            snap.tarifa().codigoMoneda(),
            snap.codigoMoneda(),
            snap.obtenidoEn()
        );
    }

    private static DetraccionResponse aRespuesta(Detraccion det) {
        return new DetraccionResponse(
            det.porcentaje(),
            det.monto().monto(),
            det.monto().codigoMoneda(),
            det.cuentaBancaria()
        );
    }

    private static ConformidadResponse aRespuesta(Conformidad conf) {
        if (conf == null) return null;
        return new ConformidadResponse(
            conf.registrada(),
            conf.incidenciasSinResolver(),
            conf.recibidaEn()
        );
    }

    private static LineaDeFacturaResponse aRespuesta(LineaDeFactura linea) {
        return new LineaDeFacturaResponse(
            linea.id(),
            linea.ordenDeServicioId(),
            linea.concepto(),
            linea.descripcion(),
            linea.importe().monto(),
            linea.importe().codigoMoneda()
        );
    }

    private static DineroResponse aRespuesta(Dinero dinero) {
        return new DineroResponse(dinero.monto(), dinero.codigoMoneda());
    }
}
