package pe.edu.unc.elmirador.comercial.models.vo;

import jakarta.persistence.Embeddable;
import pe.edu.unc.elmirador.comercial.exceptions.CondicionDePagoInconsistenteException;

/**
 * Objeto de valor inmutable que representa la condicion de pago de un servicio.
 */
@Embeddable
public record CondicionDePago(ModalidadDePago modalidad, int plazoEnDias) {

    public CondicionDePago {
        if (modalidad == null) {
            throw new IllegalArgumentException("La modalidad de pago es obligatoria");
        }
        if (modalidad == ModalidadDePago.CREDITO && plazoEnDias <= 0) {
            throw new CondicionDePagoInconsistenteException(
                "La modalidad CREDITO exige un plazo en dias mayor a cero: " + plazoEnDias
            );
        }
        if (modalidad == ModalidadDePago.CONTADO && plazoEnDias != 0) {
            throw new CondicionDePagoInconsistenteException(
                "La modalidad CONTADO exige un plazo de exactamente cero dias: " + plazoEnDias
            );
        }
    }

    public static CondicionDePago contado() {
        return new CondicionDePago(ModalidadDePago.CONTADO, 0);
    }

    public static CondicionDePago credito(int plazoEnDias) {
        return new CondicionDePago(ModalidadDePago.CREDITO, plazoEnDias);
    }

    public boolean esACredito() {
        return this.modalidad == ModalidadDePago.CREDITO;
    }

    public boolean esAlContado() {
        return this.modalidad == ModalidadDePago.CONTADO;
    }
}
