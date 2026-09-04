package pe.edu.unc.elmirador.unidades.models.vo;

public enum IntervaloDeMantenimiento {
    ACEITE_Y_FILTROS(10_000),
    REVISION_MAYOR(20_000),
    LLANTAS(40_000);

    private final int kilometros;

    IntervaloDeMantenimiento(int kilometros) {
        this.kilometros = kilometros;
    }

    public int kilometros() {
        return kilometros;
    }
}
