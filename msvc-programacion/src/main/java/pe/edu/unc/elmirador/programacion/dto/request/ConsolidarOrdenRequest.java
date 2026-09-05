package pe.edu.unc.elmirador.programacion.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ConsolidarOrdenRequest(
        @NotNull @Valid CargaRequest carga,
        @NotNull @Valid RutaRequest rutaDeLaOrden,
        @NotNull @Valid VentanaDeTiempoRequest ventanaDeLaOrden,
        @NotNull @Valid ClausulaDeConsolidacionRequest clausulaDelContrato,
        @NotNull @Valid CapacidadRequest capacidadDeLaUnidad
) {}
