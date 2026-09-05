package pe.edu.unc.elmirador.comercial.models.vo;

/**
 * Qué decidió Ejecución cuando la carga real no coincidió con la declarada. Contrato 7.
 *
 * <p>La decisión la toma Ejecución con el cliente delante; Comercial la registra y la aplica. No la
 * reevalúa: quien vio la carga fue el que estaba en el muelle.
 */
public enum DecisionDeDiferencia {

    /** Se acepta la carga real y se cobra la diferencia. */
    ACEPTADA_CON_REAJUSTE,

    /** Se acepta parte de la carga real. Tambien cambia lo que se transporta. */
    ACEPTADA_PARCIAL,

    /** No se acepta la diferencia: la orden mantiene la carga declarada. */
    RECHAZADA;

    public boolean cambiaLaCarga() {
        return this != RECHAZADA;
    }
}
