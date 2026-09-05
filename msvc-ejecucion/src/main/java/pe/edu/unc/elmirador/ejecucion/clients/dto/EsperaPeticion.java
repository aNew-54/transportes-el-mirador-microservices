package pe.edu.unc.elmirador.ejecucion.clients.dto;

public record EsperaPeticion(
        String viajeId,
        String punto,
        double tiempoLibreHoras,
        double tiempoRealHoras,
        double excedenteHoras
) {
}
