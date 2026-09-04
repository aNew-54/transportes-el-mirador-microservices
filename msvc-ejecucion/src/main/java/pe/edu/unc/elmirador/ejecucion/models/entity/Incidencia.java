package pe.edu.unc.elmirador.ejecucion.models.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Embedded;

import java.time.OffsetDateTime;
import pe.edu.unc.elmirador.ejecucion.exceptions.EvidenciaRequeridaException;
import pe.edu.unc.elmirador.ejecucion.models.vo.Evidencia;
import pe.edu.unc.elmirador.ejecucion.models.vo.TipoDeIncidencia;

@Entity
@Table(name = "incidencias")
public class Incidencia {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 30, nullable = false)
    private TipoDeIncidencia tipo;

    @Column(name = "descripcion", length = 500, nullable = false)
    private String descripcion;

    // Nula cuando el tipo no la exige. Evidencia declara sus propias columnas y su coleccion.
    @Embedded
    private Evidencia evidencia;

    @Column(name = "resuelta", nullable = false)
    private boolean resuelta;

    @Column(name = "momento", nullable = false)
    private OffsetDateTime momento;

    /** Exigido por JPA. No usar: no valida nada. */
    protected Incidencia() {
    }

    public Incidencia(
            String id,
            TipoDeIncidencia tipo,
            String descripcion,
            Evidencia evidencia,
            OffsetDateTime momento
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id de la incidencia es obligatorio");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de incidencia es obligatorio");
        }
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("La descripcion de la incidencia es obligatoria");
        }
        if (momento == null) {
            throw new IllegalArgumentException("El momento de la incidencia es obligatorio");
        }
        if (tipo.exigeEvidencia() && evidencia == null) {
            throw new EvidenciaRequeridaException(
                "La incidencia de tipo " + tipo + " exige evidencia obligatoria"
            );
        }
        this.id = id.trim();
        this.tipo = tipo;
        this.descripcion = descripcion.trim();
        this.evidencia = evidencia;
        this.resuelta = false;
        this.momento = momento;
    }

    public String getId() {
        return id;
    }

    public TipoDeIncidencia getTipo() {
        return tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Evidencia getEvidencia() {
        return evidencia;
    }

    public boolean isResuelta() {
        return resuelta;
    }

    public OffsetDateTime getMomento() {
        return momento;
    }

    public void resolver() {
        this.resuelta = true;
    }
}
