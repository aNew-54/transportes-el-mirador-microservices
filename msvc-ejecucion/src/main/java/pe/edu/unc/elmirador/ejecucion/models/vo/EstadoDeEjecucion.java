package pe.edu.unc.elmirador.ejecucion.models.vo;

import pe.edu.unc.elmirador.ejecucion.exceptions.TransicionDeEjecucionInvalidaException;

public enum EstadoDeEjecucion {
    PENDIENTE,
    EN_RUTA,
    SUSPENDIDA,
    ENTREGADA,
    CERRADA;

    public boolean puedeTransicionarHacia(EstadoDeEjecucion destino) {
        if (destino == null) {
            return false;
        }
        return switch (this) {
            case PENDIENTE -> destino == EN_RUTA;
            case EN_RUTA -> destino == SUSPENDIDA || destino == ENTREGADA;
            case SUSPENDIDA -> destino == EN_RUTA;
            case ENTREGADA -> destino == CERRADA;
            case CERRADA -> false;
        };
    }

    public void validarTransicionHacia(EstadoDeEjecucion destino) {
        if (destino == null) {
            throw new IllegalArgumentException("El estado destino es obligatorio");
        }
        if (!puedeTransicionarHacia(destino)) {
            throw new TransicionDeEjecucionInvalidaException(
                "Transicion invalida de " + this + " a " + destino
            );
        }
    }
}
