package pe.edu.unc.elmirador.ejecucion.models.vo;

import jakarta.persistence.Embeddable;
import java.time.OffsetDateTime;

@Embeddable
public record Comprobante(String tipo, String numero, OffsetDateTime fecha) {

    public Comprobante {
        if (tipo == null || tipo.isBlank()) {
            throw new IllegalArgumentException("El tipo de comprobante es obligatorio");
        }
        if (numero == null || numero.isBlank()) {
            throw new IllegalArgumentException("El numero de comprobante es obligatorio");
        }
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha del comprobante es obligatoria");
        }
        tipo = tipo.trim();
        numero = numero.trim();
    }
}
