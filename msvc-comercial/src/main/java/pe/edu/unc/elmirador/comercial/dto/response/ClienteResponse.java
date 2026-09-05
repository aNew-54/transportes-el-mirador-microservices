package pe.edu.unc.elmirador.comercial.dto.response;

public record ClienteResponse(
        String id,
        String ruc,
        String razonSocial,
        CondicionDePagoResponse condicionHabitual,
        EstadoCrediticioResponse estadoCrediticio
) {
}
