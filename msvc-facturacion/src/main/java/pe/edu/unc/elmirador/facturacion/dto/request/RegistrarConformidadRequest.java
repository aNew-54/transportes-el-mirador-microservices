package pe.edu.unc.elmirador.facturacion.dto.request;

import java.time.OffsetDateTime;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegistrarConformidadRequest(
    @NotBlank String ordenDeServicioId,
    boolean registrada,
    @NotNull List<String> incidenciasSinResolver,
    OffsetDateTime recibidaEn,
    @NotNull @Valid List<LineaDeFacturaRequest> conceptos
) {}
