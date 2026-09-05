package pe.edu.unc.elmirador.facturacion.dto.internal.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.unc.elmirador.facturacion.models.vo.ConceptoFacturable;

/**
 * Un concepto medido en ruta. Contrato 8.
 *
 * <pre>{ "concepto": "ESPERA", "monto": "245.00", "moneda": "PEN", "detalle": "3.5 h sobre 2 h" }</pre>
 *
 * <p>{@code detalle} es opcional; los otros tres no. {@code concepto} va tipado: un concepto que este
 * contexto no conoce es un {@code 400} en la frontera, con el nombre del campo, y no una excepción a
 * medio camino.
 */
public record ConceptoFacturableRequest(

        @NotNull
        ConceptoFacturable concepto,

        @NotNull
        java.math.BigDecimal monto,

        @NotBlank
        @Size(min = 3, max = 3)
        String moneda,

        String detalle
) {
}
