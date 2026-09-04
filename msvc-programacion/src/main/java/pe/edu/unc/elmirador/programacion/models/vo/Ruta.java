package pe.edu.unc.elmirador.programacion.models.vo;

import jakarta.persistence.Embeddable;

@Embeddable
public record Ruta(String origen, String destino, String corredor) {

    public Ruta {
        if (origen == null || origen.isBlank()) {
            throw new IllegalArgumentException("El origen de la ruta es obligatorio");
        }
        if (destino == null || destino.isBlank()) {
            throw new IllegalArgumentException("El destino de la ruta es obligatorio");
        }
        if (corredor == null || corredor.isBlank()) {
            throw new IllegalArgumentException("El corredor de la ruta es obligatorio");
        }
        origen = origen.trim();
        destino = destino.trim();
        corredor = corredor.trim();
    }

    public boolean mismoCorredorQue(Ruta otra) {
        if (otra == null) {
            throw new IllegalArgumentException("La otra ruta es obligatoria");
        }
        return this.corredor.equalsIgnoreCase(otra.corredor);
    }
}
