package pe.edu.unc.elmirador.programacion.models.vo;

import jakarta.persistence.Embeddable;
import java.util.List;

@Embeddable
public record HojaDeRuta(List<Parada> paradas) {

    public HojaDeRuta {
        if (paradas == null) {
            throw new IllegalArgumentException("La lista de paradas es obligatoria");
        }
        paradas = List.copyOf(paradas);
    }

    public static HojaDeRuta de(Parada... paradas) {
        if (paradas == null) {
            throw new IllegalArgumentException("Las paradas son obligatorias");
        }
        return new HojaDeRuta(List.of(paradas));
    }

    public List<Parada> secuenciaDeEstiba() {
        // TODO S1b: VIA-06 - devolver las paradas en orden inverso de descarga
        return paradas;
    }
}
