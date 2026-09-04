package pe.edu.unc.elmirador.unidades.models.vo;

import jakarta.persistence.Embeddable;
import java.util.regex.Pattern;
import pe.edu.unc.elmirador.unidades.exceptions.PlacaInvalidaException;

@Embeddable
public record Placa(String valor) {

    private static final Pattern PATRON_PLACA = Pattern.compile("^[A-Z]{3}-\\d{3}$|^[A-Z]\\d[A-Z]-\\d{3}$");

    public Placa {
        if (valor == null) {
            throw new PlacaInvalidaException("La placa no puede ser nula");
        }
        valor = valor.trim().toUpperCase();
        if (!PATRON_PLACA.matcher(valor).matches()) {
            throw new PlacaInvalidaException("Formato de placa invalido (debe ser AAA-000 o A0A-000): " + valor);
        }
    }
}
