package pe.edu.unc.elmirador.cobranza.models.vo;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Objeto de valor inmutable que representa el medio de pago utilizado.
 * La referencia bancaria u operacion es obligatoria excepto para pagos en efectivo.
 */
@Embeddable
public record MedioDePago(
    @Enumerated(EnumType.STRING) ModalidadDePago modalidad,
    String referencia
) {
    public MedioDePago {
        if (modalidad == null) {
            throw new IllegalArgumentException("La modalidad de pago es obligatoria");
        }
        if (modalidad != ModalidadDePago.EFECTIVO) {
            if (referencia == null || referencia.isBlank()) {
                throw new IllegalArgumentException("La referencia es obligatoria para la modalidad " + modalidad);
            }
            referencia = referencia.trim();
        } else {
            if (referencia != null) {
                referencia = referencia.trim();
            }
        }
    }

    public static MedioDePago efectivo() {
        return new MedioDePago(ModalidadDePago.EFECTIVO, null);
    }

    public static MedioDePago transferencia(String referencia) {
        return new MedioDePago(ModalidadDePago.TRANSFERENCIA, referencia);
    }

    public static MedioDePago deposito(String referencia) {
        return new MedioDePago(ModalidadDePago.DEPOSITO, referencia);
    }

    public static MedioDePago cheque(String referencia) {
        return new MedioDePago(ModalidadDePago.CHEQUE, referencia);
    }
}
