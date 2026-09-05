package pe.edu.unc.elmirador.programacion.dto.response;

import java.util.List;

public record HojaDeRutaResponse(
        List<ParadaResponse> paradas
) {}
