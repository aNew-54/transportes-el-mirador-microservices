package pe.edu.unc.elmirador.ejecucion.models.vo;

public enum TipoDeIncidencia {
    DANIO,
    FALTANTE,
    RECHAZO_DE_CARGA,
    AVERIA,
    DEMORA,
    CLIMA,
    BLOQUEO_DE_VIA;

    public boolean exigeEvidencia() {
        return this == DANIO || this == FALTANTE || this == RECHAZO_DE_CARGA;
    }

    /** Lo que Unidades entiende por falla en el contrato 5. Un clima o un bloqueo de via no lo son. */
    public boolean esFallaDeUnidad() {
        return this == AVERIA;
    }

    /**
     * Lo que se le reporta al conductor en el contrato 6.
     *
     * <p>La custodia de la carga es del conductor, asi que un danio o un faltante le llegan; un
     * clima, un bloqueo de via o una demora, no. Es una decision de diseno discutible, y por eso
     * se escribe con nombre aqui en vez de esconderse en un {@code if} del servicio de aplicacion:
     * quien no este de acuerdo discute con esta linea.
     */
    public boolean esImputableAlConductor() {
        return this == DANIO || this == FALTANTE;
    }
}
