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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Embeddable
public class CargaConsolidada {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "viaje_cargas",
            joinColumns = @JoinColumn(name = "viaje_id")
    )
    @AttributeOverrides({
            @AttributeOverride(name = "ordenDeServicioId", column = @Column(name = "orden_de_servicio_id", length = 40, nullable = false)),
            @AttributeOverride(name = "pesoKg", column = @Column(name = "peso_kg", nullable = false)),
            @AttributeOverride(name = "volumenM3", column = @Column(name = "volumen_m3", precision = 10, scale = 2, nullable = false)),
            @AttributeOverride(name = "tipo", column = @Column(name = "tipo", length = 20, nullable = false)),
            @AttributeOverride(name = "secuenciaDeDescarga", column = @Column(name = "secuencia_de_descarga", nullable = false))
    })
    @OrderBy("secuenciaDeDescarga ASC")
    private List<Carga> cargas = new ArrayList<>();

    /** Exigido por JPA. No usar: no valida nada. */
    protected CargaConsolidada() {
    }

    public CargaConsolidada(List<Carga> cargas) {
        if (cargas == null) {
            throw new IllegalArgumentException("La lista de cargas es obligatoria");
        }
        this.cargas.addAll(cargas);
    }

    public static CargaConsolidada vacia() {
        return new CargaConsolidada(List.of());
    }

    public static CargaConsolidada de(Carga... cargas) {
        if (cargas == null) {
            throw new IllegalArgumentException("Las cargas son obligatorias");
        }
        return new CargaConsolidada(List.of(cargas));
    }

    public CargaConsolidada agregar(Carga carga) {
        if (carga == null) {
            throw new IllegalArgumentException("La carga a agregar es obligatoria");
        }
        List<Carga> nuevaLista = new ArrayList<>(this.cargas);
        nuevaLista.add(carga);
        return new CargaConsolidada(nuevaLista);
    }

    public List<Carga> cargas() {
        return List.copyOf(cargas);
    }

    public int pesoTotal() {
        return cargas.stream()
                .mapToInt(Carga::pesoKg)
                .sum();
    }

    public BigDecimal volumenTotal() {
        return cargas.stream()
                .map(Carga::volumenM3)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean cabeEn(Capacidad capacidad) {
        if (capacidad == null) {
            throw new IllegalArgumentException("La capacidad es obligatoria");
        }
        return pesoTotal() <= capacidad.pesoMaximoKg()
                && volumenTotal().compareTo(capacidad.volumenMaximoM3()) <= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CargaConsolidada otra)) return false;
        return Objects.equals(cargas, otra.cargas);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cargas);
    }

    @Override
    public String toString() {
        return "CargaConsolidada[cargas=" + cargas + "]";
    }
}
