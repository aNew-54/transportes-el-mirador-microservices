package pe.edu.unc.elmirador.comercial.models.vo;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import pe.edu.unc.elmirador.comercial.exceptions.CondicionDePagoInconsistenteException;

/**
 * Objeto de valor inmutable que representa la condicion de pago de un servicio.
 */
@Embeddable
public record CondicionDePago(
    @Enumerated(EnumType.STRING)
    ModalidadDePago modalidad,
    int plazoEnDias
) {

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

    /**
     * Contrato 11: solo el credito obliga a preguntarle a Cobranza por el estado crediticio.
     *
     * <p>Hoy coincide con {@link #esACredito()} y aun asi son dos metodos. Uno dice de que modalidad
     * es esta condicion; el otro, si hace falta salir del contexto antes de aceptarla. El servicio
     * de aplicacion decide a quien llama, pero no es el quien decide que el credito exige verificarse:
     * esa regla es CLI-01 y vive aqui.
     */
    public boolean exigeVerificacionCrediticia() {
        return esACredito();
    }
}
