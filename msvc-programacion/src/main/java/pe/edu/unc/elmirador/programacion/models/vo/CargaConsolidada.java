package pe.edu.unc.elmirador.programacion.models.vo;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Embeddable
public record CargaConsolidada(List<Carga> cargas) {

    public CargaConsolidada {
        if (cargas == null) {
            throw new IllegalArgumentException("La lista de cargas es obligatoria");
        }
        cargas = List.copyOf(cargas);
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
        // TODO S1b: VIA-02 - verificar pesoTotal() <= capacidad.pesoMaximoKg() y volumenTotal().compareTo(capacidad.volumenMaximoM3()) <= 0
        return true;
    }
}
