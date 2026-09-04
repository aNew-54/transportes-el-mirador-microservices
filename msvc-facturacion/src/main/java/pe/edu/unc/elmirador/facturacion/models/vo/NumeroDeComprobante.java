package pe.edu.unc.elmirador.facturacion.models.vo;

import jakarta.persistence.Embeddable;
import pe.edu.unc.elmirador.facturacion.exceptions.NumeroDeComprobanteInvalidoException;

/**
 * Objeto de valor inmutable que representa el numero de comprobante de pago (serie + correlativo).
 * Serie debe ser F o B seguido de tres digitos (ej. F001, B001). Correlativo positivo sin saltos.
 */
@Embeddable
public record NumeroDeComprobante(String serie, int correlativo) {

    public NumeroDeComprobante {
        if (serie == null || serie.isBlank()) {
            throw new NumeroDeComprobanteInvalidoException("La serie del comprobante es obligatoria");
        }
        String serieNormalizada = serie.trim().toUpperCase();
        if (!serieNormalizada.matches("^[FB]\\d{3}$")) {
            throw new NumeroDeComprobanteInvalidoException(
                "La serie debe iniciar con F o B seguido de 3 digitos (ej. F001): " + serie
            );
        }
        if (correlativo <= 0) {
            throw new NumeroDeComprobanteInvalidoException(
                "El correlativo debe ser positivo: " + correlativo
            );
        }
        serie = serieNormalizada;
    }

    public static NumeroDeComprobante de(String serie, int correlativo) {
        return new NumeroDeComprobante(serie, correlativo);
    }

    public static NumeroDeComprobante de(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new NumeroDeComprobanteInvalidoException("El texto del comprobante es obligatorio");
        }
        String[] partes = texto.trim().split("-");
        if (partes.length != 2) {
            throw new NumeroDeComprobanteInvalidoException(
                "Formato de comprobante invalido, se esperaba SERIE-CORRELATIVO: " + texto
            );
        }
        try {
            int corr = Integer.parseInt(partes[1]);
            return new NumeroDeComprobante(partes[0], corr);
        } catch (NumberFormatException e) {
            throw new NumeroDeComprobanteInvalidoException(
                "El correlativo debe ser numerico: " + partes[1], e
            );
        }
    }

    public NumeroDeComprobante siguiente() {
        return new NumeroDeComprobante(this.serie, this.correlativo + 1);
    }

    public String formateado() {
        return String.format("%s-%08d", this.serie, this.correlativo);
    }
}
