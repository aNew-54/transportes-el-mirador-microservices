package pe.edu.unc.elmirador.programacion.models.vo;

public enum EstadoDeReserva {
    TENTATIVA,
    CONFIRMADA,
    LIBERADA;

    public boolean bloqueaElRecurso() {
        return this == TENTATIVA || this == CONFIRMADA;
    }
}
