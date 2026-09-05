package pe.edu.unc.elmirador.programacion.dto.response;

import java.util.List;
import pe.edu.unc.elmirador.programacion.models.vo.EstadoDeViaje;

public record ViajeResponse(
        String id,
        RutaResponse ruta,
        VentanaDeTiempoResponse ventana,
        CargaConsolidadaResponse cargaConsolidada,
        AsignacionDeRecursosResponse asignacionDeRecursos,
        EstadoDeViaje estado,
        HojaDeRutaResponse hojaDeRuta,
        List<String> ordenIds
) {}
