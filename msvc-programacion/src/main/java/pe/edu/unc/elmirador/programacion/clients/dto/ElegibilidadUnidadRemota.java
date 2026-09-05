package pe.edu.unc.elmirador.programacion.clients.dto;

import java.util.List;

public record ElegibilidadUnidadRemota(
    String unidadId,
    boolean elegible,
    List<String> motivos,
    CapacidadRemota capacidad,
    String tipoUnidad,
    String estadoOperativo
) {}
