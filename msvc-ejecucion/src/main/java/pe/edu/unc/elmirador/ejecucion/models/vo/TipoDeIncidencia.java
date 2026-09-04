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
}
