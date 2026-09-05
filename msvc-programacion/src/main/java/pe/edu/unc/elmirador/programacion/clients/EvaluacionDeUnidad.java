package pe.edu.unc.elmirador.programacion.clients;

import pe.edu.unc.elmirador.programacion.models.vo.Capacidad;
import pe.edu.unc.elmirador.programacion.models.vo.ElegibilidadDeRecurso;

public record EvaluacionDeUnidad(
        ElegibilidadDeRecurso elegibilidad,
        Capacidad capacidad,
        String tipoUnidad
) {}
