package pe.edu.unc.elmirador.programacion.models.vo;

public enum TipoDeCarga {
    PALETIZADA,
    GENERAL,
    MAQUINARIA_PESADA;

    /**
     * VIA-05: compatibilidad fisica entre dos cargas que compartirian plataforma.
     *
     * <p>La maquinaria pesada no comparte viaje con nada mas: ocupa la plataforma entera y su
     * amarre no admite carga encima. Paletizada y general si conviven. La relacion es simetrica,
     * y la prueba la recorre en los dos sentidos.
     */
    public boolean esCompatibleCon(TipoDeCarga otro) {
        if (otro == null) {
            throw new IllegalArgumentException("El otro tipo de carga es obligatorio");
        }
        if (this == MAQUINARIA_PESADA || otro == MAQUINARIA_PESADA) {
            return this == otro;
        }
        return true;
    }
}
