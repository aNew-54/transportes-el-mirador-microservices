package pe.edu.unc.elmirador.cobranza.models.vo;

/**
 * Cómo se pactó el cobro de la factura. Llega por el contrato 10.
 *
 * <p>No confundir con {@link ModalidadDePago}, que dice el medio con el que un cliente paga
 * —efectivo, transferencia, deposito, cheque—. Esto dice si hay algo que cobrar despues.
 */
public enum CondicionDeVenta {

    /** Se cobra contra entrega: la cuenta entra a la cartera ya cancelada. */
    CONTADO,

    /** Se cobra a plazo: la cuenta entra viva y envejece hasta que se paga. */
    CREDITO
}
