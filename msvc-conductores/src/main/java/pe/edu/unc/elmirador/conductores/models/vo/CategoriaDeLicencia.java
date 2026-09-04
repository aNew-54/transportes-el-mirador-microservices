package pe.edu.unc.elmirador.conductores.models.vo;

public enum CategoriaDeLicencia {
    A_IIIA,
    A_IIIB,
    A_IIIC;

    public boolean habilitaPara(TipoDeUnidad tipo) {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de unidad no puede ser nulo");
        }
        return switch (this) {
            case A_IIIA -> tipo == TipoDeUnidad.FURGON;
            case A_IIIB -> tipo == TipoDeUnidad.FURGON || tipo == TipoDeUnidad.PLATAFORMA;
            case A_IIIC -> true;
        };
    }
}
