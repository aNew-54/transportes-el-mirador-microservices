package pe.edu.unc.elmirador.conductores.models.vo;

import jakarta.persistence.Embeddable;
import pe.edu.unc.elmirador.conductores.exceptions.NumeroDeLicenciaInvalidoException;

import java.util.regex.Pattern;

@Embeddable
public record NumeroDeLicencia(String valor) {

    private static final Pattern PATRON_LICENCIA = Pattern.compile("^[A-Z]\\d{8}$");

    public NumeroDeLicencia {
        if (valor == null) {
            throw new NumeroDeLicenciaInvalidoException("El numero de licencia no puede ser nulo");
        }
        valor = valor.trim().toUpperCase();
        if (!PATRON_LICENCIA.matcher(valor).matches()) {
            throw new NumeroDeLicenciaInvalidoException(
                    "El formato del numero de licencia es invalido: " + valor
            );
        }
    }
}
