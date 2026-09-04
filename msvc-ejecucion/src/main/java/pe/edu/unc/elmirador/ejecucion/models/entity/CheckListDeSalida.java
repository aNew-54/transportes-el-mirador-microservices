package pe.edu.unc.elmirador.ejecucion.models.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Embedded;

import pe.edu.unc.elmirador.ejecucion.models.vo.ResultadoDeCheckList;

@Entity
@Table(name = "checklists")
public class CheckListDeSalida {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Embedded
    private ResultadoDeCheckList resultado;

    /** Exigido por JPA. No usar: no valida nada. */
    protected CheckListDeSalida() {
    }

    public CheckListDeSalida(String id, ResultadoDeCheckList resultado) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id del check-list es obligatorio");
        }
        if (resultado == null) {
            throw new IllegalArgumentException("El resultado del check-list es obligatorio");
        }
        this.id = id.trim();
        this.resultado = resultado;
    }

    public String getId() {
        return id;
    }

    public ResultadoDeCheckList getResultado() {
        return resultado;
    }

    public boolean estaAprobado() {
        return resultado.aprobado();
    }
}
