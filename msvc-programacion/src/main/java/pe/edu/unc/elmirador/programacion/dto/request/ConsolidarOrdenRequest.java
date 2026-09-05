package pe.edu.unc.elmirador.programacion.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Consolidar una orden en un viaje.
 *
 * <p>Antes esta peticion traia la carga, la ruta, la ventana y la clausula del contrato marco. Es
 * decir: quien pedia consolidar aportaba tambien la clausula que decide si se puede consolidar, con
 * lo que VIA-04 se comprobaba contra un dato del propio solicitante y bastaba con enviar una clausula
 * permisiva. Ahora todo eso lo trae el contrato 1 desde Comercial y aqui solo viaja el identificador.
 *
 * <p>Lo que si es de quien pide: donde va esta orden en la estiba, y contra que capacidad se
 * comprueba que quepa. La unidad todavia no esta asignada en este punto del ciclo de vida.
 */
public record ConsolidarOrdenRequest(
        @NotBlank String ordenId,
        @NotNull @Min(1) Integer secuenciaDeDescarga,
        @NotNull @Valid CapacidadRequest capacidadDeLaUnidad
) {}
