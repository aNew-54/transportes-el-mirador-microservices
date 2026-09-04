package pe.edu.unc.elmirador.ejecucion.models.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import pe.edu.unc.elmirador.ejecucion.models.vo.TipoDeHito;

@Entity
@Table(name = "hitos")
public class Hito {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 30, nullable = false)
    private TipoDeHito tipo;

    @Column(name = "momento", nullable = false)
    private OffsetDateTime momento;

    @Column(name = "ubicacion", length = 300)
    private String ubicacion;

    /** Exigido por JPA. No usar: no valida nada. */
    protected Hito() {
    }

    public Hito(String id, TipoDeHito tipo, OffsetDateTime momento, String ubicacion) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id del hito es obligatorio");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de hito es obligatorio");
        }
        if (momento == null) {
            throw new IllegalArgumentException("El momento del hito es obligatorio");
        }
        if (ubicacion == null || ubicacion.isBlank()) {
            throw new IllegalArgumentException("La ubicacion del hito es obligatoria");
        }
        this.id = id.trim();
        this.tipo = tipo;
        this.momento = momento;
        this.ubicacion = ubicacion.trim();
    }

    public String getId() {
        return id;
    }

    public TipoDeHito getTipo() {
        return tipo;
    }

    public OffsetDateTime getMomento() {
        return momento;
    }

    public String getUbicacion() {
        return ubicacion;
    }
}
