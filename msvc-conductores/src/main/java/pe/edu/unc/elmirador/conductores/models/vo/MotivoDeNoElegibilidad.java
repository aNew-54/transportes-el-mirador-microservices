package pe.edu.unc.elmirador.conductores.models.vo;

public enum MotivoDeNoElegibilidad {
    LICENCIA_VENCIDA,
    CATEGORIA_INSUFICIENTE,
    HORAS_INSUFICIENTES,
    INDUCCION_VENCIDA,
    NO_HABILITADO;

    public String codigo() {
        return name();
    }

    public String codigo(String detalle) {
        if (detalle == null || detalle.isBlank()) {
            throw new IllegalArgumentException("El detalle no puede ser nulo ni vacio");
        }
        return name() + ":" + detalle.trim();
    }
}
