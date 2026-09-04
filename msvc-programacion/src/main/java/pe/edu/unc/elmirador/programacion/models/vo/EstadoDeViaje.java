package pe.edu.unc.elmirador.programacion.models.vo;

import pe.edu.unc.elmirador.programacion.exceptions.TransicionDeViajeInvalidaException;

public enum EstadoDeViaje {
    PLANIFICADO,
    PROGRAMADO,
    DESPACHADO,
    CANCELADO;

    public boolean puedeTransicionarA(EstadoDeViaje nuevoEstado) {
        if (nuevoEstado == null) {
            throw new IllegalArgumentException("El nuevo estado de viaje es obligatorio");
        }
        return switch (this) {
            case PLANIFICADO -> nuevoEstado == PROGRAMADO || nuevoEstado == CANCELADO;
            case PROGRAMADO -> nuevoEstado == DESPACHADO || nuevoEstado == CANCELADO;
            case DESPACHADO, CANCELADO -> false;
        };
    }

    public void validarTransicion(EstadoDeViaje nuevoEstado) {
        if (!puedeTransicionarA(nuevoEstado)) {
            throw new TransicionDeViajeInvalidaException(
                "Transicion no permitida de " + this + " a " + nuevoEstado
            );
        }
    }
}
