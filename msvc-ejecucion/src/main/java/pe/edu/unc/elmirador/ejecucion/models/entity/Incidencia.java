package pe.edu.unc.elmirador.ejecucion.models.entity;

import java.time.OffsetDateTime;
import pe.edu.unc.elmirador.ejecucion.exceptions.EvidenciaRequeridaException;
import pe.edu.unc.elmirador.ejecucion.models.vo.Evidencia;
import pe.edu.unc.elmirador.ejecucion.models.vo.TipoDeIncidencia;

public class Incidencia {

    private final String id;
    private final TipoDeIncidencia tipo;
    private final String descripcion;
    private final Evidencia evidencia;
    private boolean resuelta;
    private final OffsetDateTime momento;

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
