package pe.edu.unc.elmirador.ejecucion.models.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import pe.edu.unc.elmirador.ejecucion.models.vo.EstadoConformidad;

@Entity
@Table(name = "conformidades")
public class ConformidadDeEntrega {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Column(name = "orden_de_servicio_id", length = 40, nullable = false)
    private String ordenDeServicioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoConformidad estado;

    @Column(name = "recibido_por", length = 200)
    private String recibidoPor;

    @Column(name = "fecha_de_firma")
    private OffsetDateTime fechaDeFirma;

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    /** Exigido por JPA. No usar: no valida nada. */
    protected ConformidadDeEntrega() {
    }

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
