package pe.edu.unc.elmirador.programacion.models.vo;

import jakarta.persistence.Embeddable;
import java.util.Comparator;
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

    /**
     * VIA-06: la carga que se descarga primero se estiba al final.
     *
     * <p>Devuelve las paradas de descarga en orden inverso al de su secuencia. Sólo entran las de
     * tipo {@code DESCARGA}: una parada de carga no tiene orden de descarga contra el que estibar,
     * y el vocabulario lo fija el contrato 4, no esta clase.
     */
    public List<Parada> secuenciaDeEstiba() {
        return paradas.stream()
                .filter(Parada::esDescarga)
                .sorted(Comparator.comparingInt(Parada::secuencia).reversed())
                .toList();
    }
}
