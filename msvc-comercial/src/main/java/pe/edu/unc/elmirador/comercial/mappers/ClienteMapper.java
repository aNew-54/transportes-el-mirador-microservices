package pe.edu.unc.elmirador.comercial.mappers;

import pe.edu.unc.elmirador.comercial.dto.response.ClienteResponse;
import pe.edu.unc.elmirador.comercial.dto.response.CondicionDePagoResponse;
import pe.edu.unc.elmirador.comercial.dto.response.EstadoCrediticioResponse;
import pe.edu.unc.elmirador.comercial.models.entity.Cliente;

public final class ClienteMapper {

    private ClienteMapper() {
    }

    public static ClienteResponse aRespuesta(Cliente cliente) {
        return new ClienteResponse(
                cliente.id(),
                cliente.ruc().valor(),
                cliente.razonSocial().valor(),
                new CondicionDePagoResponse(
                        cliente.condicionHabitual().modalidad().name(),
                        cliente.condicionHabitual().plazoEnDias()
                ),
                cliente.estadoCrediticio() != null ? new EstadoCrediticioResponse(
                        cliente.estadoCrediticio().situacion().name(),
                        cliente.estadoCrediticio().fechaDeCambio()
                ) : null
        );
    }
}
