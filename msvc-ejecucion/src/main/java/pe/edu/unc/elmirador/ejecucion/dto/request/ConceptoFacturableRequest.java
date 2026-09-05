package pe.edu.unc.elmirador.ejecucion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Un concepto facturable ya tarifado, para el contrato 8.
 *
 * <p>El importe viene en el cuerpo porque Ejecucion no tiene tarifario: sabe cuantas horas de
 * espera hubo —lo calcula {@code EsperaFacturable.excedente()}— pero no a cuanto se cobra la hora.
 * Lo que Ejecucion no acepta del cuerpo es la orden a la que se imputa sin comprobarla: el
 * {@code ordenDeServicioId} tiene que corresponder a una parada de esta ejecucion.
 */
public record ConceptoFacturableRequest(
        @NotBlank String ordenDeServicioId,
        @NotBlank String concepto,
        @NotBlank @Pattern(regexp = "^\\d+(\\.\\d{1,2})?$",
                message = "El monto debe ser un decimal no negativo con hasta dos decimales") String monto,
        @NotBlank @Pattern(regexp = "^[A-Za-z]{3}$",
                message = "La moneda debe ser un codigo ISO-4217 de tres letras") String moneda,
        String detalle
) {
}
