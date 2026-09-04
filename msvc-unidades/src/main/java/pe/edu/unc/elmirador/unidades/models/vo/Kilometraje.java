package pe.edu.unc.elmirador.unidades.models.vo;

import jakarta.persistence.Embeddable;
import pe.edu.unc.elmirador.unidades.exceptions.KilometrajeRetrocedeException;

@Embeddable
public record Kilometraje(int valor) implements Comparable<Kilometraje> {

    public Kilometraje {
        if (valor < 0) {
            throw new IllegalArgumentException("El kilometraje no puede ser negativo: " + valor);
        }
    }

    public Kilometraje avanzarA(Kilometraje nuevo) {
        if (nuevo == null) {
            throw new IllegalArgumentException("El nuevo kilometraje no puede ser nulo");
        }
        if (nuevo.valor < this.valor) {
            throw new KilometrajeRetrocedeException(
                    "El kilometraje no puede decrecer (UNI-03): actual=" + this.valor + ", nuevo=" + nuevo.valor);
        }
        return nuevo;
    }

    @Override
    public int compareTo(Kilometraje otro) {
        if (otro == null) {
            throw new IllegalArgumentException("No se puede comparar con un kilometraje nulo");
        }
        return Integer.compare(this.valor, otro.valor);
    }
}
