package pe.edu.unc.elmirador.unidades.models.vo;

public enum MotivoDeNoElegibilidad {
    DOCUMENTO_VENCIDO,
    MANTENIMIENTO_VENCIDO,
    EN_TALLER,
    INOPERATIVA,
    CAPACIDAD_INSUFICIENTE,
    TIPO_INCOMPATIBLE;

    public String codigo() {
        return this.name();
    }

    public String codigo(String detalle) {
        if (detalle == null || detalle.isBlank()) {
            return codigo();
        }
        return this.name() + ":" + detalle.trim();
    }
}
