package pe.edu.unc.elmirador.cobranza.models.vo;

/**
 * Tramo de accion de cobranza correspondiente a los dias de atraso de una cuenta.
 */
public enum TramoDeGestion {
    SIN_ACCION,
    RECORDATORIO,
    LLAMADA_DE_SEGUIMIENTO,
    COMUNICACION_FORMAL,
    SUSPENSION_DE_CREDITO
}
