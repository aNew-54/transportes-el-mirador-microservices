package pe.edu.unc.elmirador.facturacion.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import pe.edu.unc.elmirador.facturacion.models.vo.EstadoDeFactura;

public record FacturaResponse(
    String id,
    String ordenDeServicioId,
    String clienteId,
    String numeroDeComprobante,
    SnapshotComercialResponse snapshotComercial,
    DetraccionResponse detraccion,
    ConformidadResponse conformidad,
    EstadoDeFactura estado,
    OffsetDateTime fechaDeEmision,
    boolean falsoFlete,
    BigDecimal totalMonto,
    String totalMoneda,
    BigDecimal montoNetoMonto,
    String montoNetoMoneda,
    BigDecimal saldoAjustableMonto,
    String saldoAjustableMoneda,
    List<LineaDeFacturaResponse> lineas,
    List<DineroResponse> ajustesAplicados
) {}
