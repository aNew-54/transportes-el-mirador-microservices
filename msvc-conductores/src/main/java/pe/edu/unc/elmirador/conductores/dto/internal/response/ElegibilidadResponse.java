package pe.edu.unc.elmirador.conductores.dto.internal.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Contrato 3 · Programacion → Conductores. Sostiene AGC-02.
 *
 * <p>{@code elegible: false} es una respuesta {@code 200} valida, no un error: Programacion registra
 * el motivo y busca otro conductor. Devolver un {@code 404} o un {@code 422} obligaria al consumidor a
 * leer un error como una respuesta de negocio, que es lo que la regla 5 prohibe.
 *
 * <p>{@code categoriaLicencia} viaja con guion —{@code A-IIIB}— porque asi lo fija el contrato. El
 * enumerado del dominio usa guion bajo por la regla 13, y la conversion vive en el mapeador.
 */
public record ElegibilidadResponse(
        String conductorId,
        boolean elegible,
        List<String> motivos,
        String categoriaLicencia,
        BigDecimal horasDisponibles
) {
}
