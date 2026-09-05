package pe.edu.unc.elmirador.programacion.clients.dto;

import java.util.List;
import java.math.BigDecimal;

public record ElegibilidadConductorRemota(
    String conductorId,
    boolean elegible,
    List<String> motivos,
    String categoriaLicencia,
    BigDecimal horasDisponibles
) {}
