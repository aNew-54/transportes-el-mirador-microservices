package pe.edu.unc.elmirador.ejecucion.models.entity;

import java.time.OffsetDateTime;
import pe.edu.unc.elmirador.ejecucion.models.vo.EstadoConformidad;

public class ConformidadDeEntrega {

    private final String id;
    private final String ordenDeServicioId;
    private final EstadoConformidad estado;
    private final String recibidoPor;
    private final OffsetDateTime fechaDeFirma;
    private final String observaciones;

    public ConformidadDeEntrega(
            String id,
            String ordenDeServicioId,
            EstadoConformidad estado,
            String recibidoPor,
            OffsetDateTime fechaDeFirma,
            String observaciones
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id de la conformidad es obligatorio");
        }
        if (ordenDeServicioId == null || ordenDeServicioId.isBlank()) {
            throw new IllegalArgumentException("La orden de servicio es obligatoria");
        }
        if (estado == null) {
            throw new IllegalArgumentException("El estado de la conformidad es obligatorio");
        }
        if (recibidoPor == null || recibidoPor.isBlank()) {
            throw new IllegalArgumentException("El receptor es obligatorio");
        }
        if (fechaDeFirma == null) {
            throw new IllegalArgumentException("La fecha de firma es obligatoria");
        }
        this.id = id.trim();
        this.ordenDeServicioId = ordenDeServicioId.trim();
        this.estado = estado;
        this.recibidoPor = recibidoPor.trim();
        this.fechaDeFirma = fechaDeFirma;
        this.observaciones = observaciones != null ? observaciones.trim() : "";
    }

    public String getId() {
        return id;
    }

    public String getOrdenDeServicioId() {
        return ordenDeServicioId;
    }

    public EstadoConformidad getEstado() {
        return estado;
    }

    public String getRecibidoPor() {
        return recibidoPor;
    }

    public OffsetDateTime getFechaDeFirma() {
        return fechaDeFirma;
    }

    public OffsetDateTime getMomento() {
        return fechaDeFirma;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public boolean estaFirmada() {
        return estado == EstadoConformidad.FIRMADA;
    }
}
