package pe.edu.unc.elmirador.programacion.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlanificarViajeRequest(
        @NotBlank @Size(max = 40) String id,
        @NotNull @Valid RutaRequest ruta,
        @NotNull @Valid VentanaDeTiempoRequest ventana,
        @NotNull @Valid CargaRequest cargaInicial
) {}
