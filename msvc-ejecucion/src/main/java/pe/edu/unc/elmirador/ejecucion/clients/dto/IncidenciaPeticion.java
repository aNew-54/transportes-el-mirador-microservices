package pe.edu.unc.elmirador.ejecucion.clients.dto;

public record IncidenciaPeticion(
        String viajeId,
        String tipo,
        String descripcion,
        boolean atribuible
) {
}
