package pe.edu.unc.elmirador.programacion.dto.internal.response;

/** Contrato 4. Los cuatro campos que Ejecucion necesita para llegar y entregar. */
public record UbicacionContratoResponse(
        String direccion,
        String distrito,
        String referencia,
        String contacto
) {
}
