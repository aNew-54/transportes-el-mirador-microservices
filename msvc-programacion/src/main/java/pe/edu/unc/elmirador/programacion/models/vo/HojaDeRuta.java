package pe.edu.unc.elmirador.programacion.models.vo;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Embeddable
public class HojaDeRuta {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "viaje_paradas",
            joinColumns = @JoinColumn(name = "viaje_id")
    )
    @AttributeOverrides({
            @AttributeOverride(name = "secuencia", column = @Column(name = "secuencia", nullable = false)),
            @AttributeOverride(name = "tipo", column = @Column(name = "tipo", length = 20, nullable = false)),
            @AttributeOverride(name = "ordenDeServicioId", column = @Column(name = "orden_de_servicio_id", length = 40, nullable = false)),
            @AttributeOverride(name = "ubicacion", column = @Column(name = "ubicacion", length = 300)),
            @AttributeOverride(name = "horaEstimada", column = @Column(name = "hora_estimada"))
    })
    @OrderBy("secuencia ASC")
    private List<Parada> paradas = new ArrayList<>();

    /** Exigido por JPA. No usar: no valida nada. */
    protected HojaDeRuta() {
    }

    public HojaDeRuta(List<Parada> paradas) {
        if (paradas == null) {
            throw new IllegalArgumentException("La lista de paradas es obligatoria");
        }
        this.paradas.addAll(paradas);
    }

    public static HojaDeRuta de(Parada... paradas) {
        if (paradas == null) {
            throw new IllegalArgumentException("Las paradas son obligatorias");
        }
        return new HojaDeRuta(List.of(paradas));
    }

    public List<Parada> paradas() {
        return List.copyOf(paradas);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HojaDeRuta otra)) return false;
        return Objects.equals(paradas, otra.paradas);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paradas);
    }

    @Override
    public String toString() {
        return "HojaDeRuta[paradas=" + paradas + "]";
    }
}
