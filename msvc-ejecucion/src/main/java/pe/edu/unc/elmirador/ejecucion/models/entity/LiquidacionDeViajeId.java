package pe.edu.unc.elmirador.ejecucion.models.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * Identidad compuesta de la liquidacion: el viaje y el conductor.
 *
 * <p>En un viaje con relevo hay dos liquidaciones independientes sobre el mismo viaje, una por
 * conductor. Esa es la razon de que la clave sea compuesta y no un identificador suelto.
 */
public class LiquidacionDeViajeId implements Serializable {

    private String viajeId;
    private String conductorId;

    protected LiquidacionDeViajeId() {
    }

    public LiquidacionDeViajeId(String viajeId, String conductorId) {
        this.viajeId = viajeId;
        this.conductorId = conductorId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LiquidacionDeViajeId otra)) {
            return false;
        }
        return Objects.equals(viajeId, otra.viajeId) && Objects.equals(conductorId, otra.conductorId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(viajeId, conductorId);
    }
}
