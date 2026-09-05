package pe.edu.unc.elmirador.ejecucion.clients.dto;

import java.time.OffsetDateTime;

public record DiferenciaCargaPeticion(
        String viajeId,
        CargaRemota declarado,
        CargaRemota real,
        String decision,
        ImporteRemoto importeDelReajuste,
        OffsetDateTime momento
) {
}
