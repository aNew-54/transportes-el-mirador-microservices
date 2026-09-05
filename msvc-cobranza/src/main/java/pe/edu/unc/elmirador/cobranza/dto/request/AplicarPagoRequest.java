package pe.edu.unc.elmirador.cobranza.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record AplicarPagoRequest(
        @NotEmpty @Valid List<AplicacionRequest> aplicaciones
) {
}
