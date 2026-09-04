package pe.edu.unc.elmirador.facturacion.models.vo;

import jakarta.persistence.Embeddable;
import java.time.OffsetDateTime;
import pe.edu.unc.elmirador.facturacion.exceptions.MonedaIncompatibleException;

/**
 * Objeto de valor inmutable que preserva la tarifa y condiciones comerciales al momento de abrir la factura.
 * Inmutable y local: un cambio posterior de tarifa en Comercial no altera una factura emitida.
 */
@Embeddable
public record SnapshotComercial(
    String ordenDeServicioId,
    String clienteId,
    Dinero tarifa,
    String codigoMoneda,
    OffsetDateTime obtenidoEn
) {
    public SnapshotComercial {
        if (ordenDeServicioId == null || ordenDeServicioId.isBlank()) {
            throw new IllegalArgumentException("El ordenDeServicioId es obligatorio");
        }
        if (clienteId == null || clienteId.isBlank()) {
            throw new IllegalArgumentException("El clienteId es obligatorio");
        }
        if (tarifa == null) {
            throw new IllegalArgumentException("La tarifa es obligatoria");
        }
        if (codigoMoneda == null || codigoMoneda.isBlank()) {
            throw new IllegalArgumentException("El codigo de moneda es obligatorio");
        }
        String monedaNormalizada = codigoMoneda.trim().toUpperCase();
        if (!monedaNormalizada.matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException("El codigo de moneda debe ser ISO-4217 de 3 letras: " + codigoMoneda);
        }
        if (!tarifa.codigoMoneda().equalsIgnoreCase(monedaNormalizada)) {
            throw new MonedaIncompatibleException(
                "La moneda de la tarifa (" + tarifa.codigoMoneda() + ") no coincide con el codigo declarado (" + monedaNormalizada + ")"
            );
        }
        if (obtenidoEn == null) {
            throw new IllegalArgumentException("El instante de obtencion es obligatorio");
        }
        ordenDeServicioId = ordenDeServicioId.trim();
        clienteId = clienteId.trim();
        codigoMoneda = monedaNormalizada;
    }
}
