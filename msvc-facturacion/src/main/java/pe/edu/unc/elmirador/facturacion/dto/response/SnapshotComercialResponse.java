package pe.edu.unc.elmirador.facturacion.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SnapshotComercialResponse(
    String ordenDeServicioId,
    String clienteId,
    BigDecimal tarifaMonto,
    String tarifaMoneda,
    String codigoMoneda,
    OffsetDateTime obtenidoEn
) {}
