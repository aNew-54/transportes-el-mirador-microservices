package pe.edu.unc.elmirador.ejecucion.models.entity;

import java.time.OffsetDateTime;
import pe.edu.unc.elmirador.ejecucion.models.vo.TipoDeHito;

public class Hito {

    private final String id;
    private final TipoDeHito tipo;
    private final OffsetDateTime momento;
    private final String ubicacion;

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
