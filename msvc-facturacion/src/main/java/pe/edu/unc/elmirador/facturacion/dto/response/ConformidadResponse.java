package pe.edu.unc.elmirador.facturacion.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record ConformidadResponse(
    boolean registrada,
    List<String> incidenciasSinResolver,
    OffsetDateTime recibidaEn
) {}
