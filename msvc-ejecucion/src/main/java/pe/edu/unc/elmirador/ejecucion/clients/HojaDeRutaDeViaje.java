package pe.edu.unc.elmirador.ejecucion.clients;

import java.util.List;

/**
 * La hoja de ruta del contrato 4, traducida al idioma de Ejecucion.
 *
 * <p>El gateway no devuelve el DTO remoto: si lo hiciera, la forma de Programacion entraria en el
 * servicio de aplicacion y la barrera anticorrupcion no estaria barriendo nada.
 */
public record HojaDeRutaDeViaje(
        String viajeId,
        String estado,
        String unidadId,
        List<String> conductorIds,
        List<ParadaPlanificada> paradas
) {

    /** Una parada tal como Programacion la planifico. Ejecucion le anade despues lo que pase. */
    public record ParadaPlanificada(int secuencia, String ordenDeServicioId, String direccion) {}
}
