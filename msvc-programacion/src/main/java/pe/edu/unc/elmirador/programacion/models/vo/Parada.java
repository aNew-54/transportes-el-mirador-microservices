package pe.edu.unc.elmirador.programacion.models.vo;

import jakarta.persistence.Embeddable;
import java.time.OffsetDateTime;

@Embeddable
public record Parada(
        int secuencia,
        String tipo,
        String ordenDeServicioId,
        String ubicacion,
        OffsetDateTime horaEstimada) {

    public Parada {
        if (secuencia <= 0) {
            throw new IllegalArgumentException("La secuencia de la parada debe ser mayor a cero: " + secuencia);
        }
        if (tipo == null || tipo.isBlank()) {
            throw new IllegalArgumentException("El tipo de parada es obligatorio");
        }
        if (ordenDeServicioId == null || ordenDeServicioId.isBlank()) {
            throw new IllegalArgumentException("El ordenDeServicioId es obligatorio");
        }
        tipo = tipo.trim().toUpperCase();
        ordenDeServicioId = ordenDeServicioId.trim();
        ubicacion = (ubicacion != null) ? ubicacion.trim() : "";
    }

    public static Parada de(int secuencia, String tipo, String ordenDeServicioId) {
        return new Parada(secuencia, tipo, ordenDeServicioId, "", null);
    }
}
