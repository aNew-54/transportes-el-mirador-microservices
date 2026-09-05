package pe.edu.unc.elmirador.cobranza.dto.internal.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import pe.edu.unc.elmirador.cobranza.models.vo.CondicionDeVenta;

/**
 * Condicion pactada de la factura. Contrato 10.
 *
 * <p>{@code modalidad} va tipada como {@link CondicionDeVenta} y no como texto: un valor que no sea
 * CONTADO ni CREDITO es un `400` en la frontera, no un contado silencioso mas adelante.
 */
public record CondicionDePagoRequest(

        @NotNull
        CondicionDeVenta modalidad,

        @NotNull @PositiveOrZero
        Integer plazoEnDias
) {
}
