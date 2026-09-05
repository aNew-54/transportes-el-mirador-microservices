package pe.edu.unc.elmirador.cobranza.dto.internal.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * La detraccion tal como llega en el contrato 10.
 *
 * <p>{@code cuentaBancaria} no lleva {@code @NotBlank}. Lo llevaba, y eso hacia estructuralmente
 * imposible registrar una factura sin detraccion: la detraccion solo aplica por encima de un umbral
 * y por debajo de el no hay ni monto ni cuenta donde depositarlo. Facturacion emitia esas facturas
 * sin problema —su propio DTO deja la cuenta opcional— y Cobranza las rechazaba con un 400, asi que
 * los dos contextos no estaban de acuerdo sobre si la detraccion es obligatoria. El contrato tampoco
 * lo dice: su ejemplo es un caso con detraccion, no una exigencia.
 *
 * <p>La regla real es condicional y por eso se escribe como metodo y no como anotacion: si hay monto
 * detraido, tiene que haber cuenta donde se deposito.
 */
public record DetraccionRequest(
        @NotNull BigDecimal porcentaje,
        @NotBlank String monto,
        @NotBlank String moneda,
        String cuentaBancaria
) {

    /** Hay detraccion cuando se detrajo algo. Un cero no es una detraccion pequena: no la hay. */
    public boolean hayDetraccion() {
        return new BigDecimal(monto).signum() > 0;
    }

    /** Si se detrajo dinero, el contrato tiene que decir a que cuenta fue. */
    public boolean tieneCuentaCuandoHaceFalta() {
        return !hayDetraccion() || (cuentaBancaria != null && !cuentaBancaria.isBlank());
    }
}
