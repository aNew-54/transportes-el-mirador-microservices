package pe.edu.unc.elmirador.conductores.models.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import pe.edu.unc.elmirador.conductores.models.vo.PeriodoDeVigencia;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "inducciones")
public class Induccion {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Column(name = "cliente_id", length = 40, nullable = false)
    private String clienteId;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "desde", column = @Column(name = "vigente_desde", nullable = false)),
        @AttributeOverride(name = "hasta", column = @Column(name = "vigente_hasta", nullable = false))
    })
    private PeriodoDeVigencia vigencia;

    /** Exigido por JPA. No usar: no valida ninguna invariante. */
    protected Induccion() {
    }

    public Induccion(String id, String clienteId, PeriodoDeVigencia vigencia) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id de la induccion no puede ser nulo ni vacio");
        }
        if (clienteId == null || clienteId.isBlank()) {
            throw new IllegalArgumentException("El clienteId no puede ser nulo ni vacio");
        }
        if (vigencia == null) {
            throw new IllegalArgumentException("El periodo de vigencia no puede ser nulo");
        }
        this.id = id.trim();
        this.clienteId = clienteId.trim();
        this.vigencia = vigencia;
    }

    public String getId() {
        return id;
    }

    public String getClienteId() {
        return clienteId;
    }

    public PeriodoDeVigencia getVigencia() {
        return vigencia;
    }

    public boolean estaVigenteEn(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha no puede ser nula");
        }
        return vigencia.estaVigenteEn(fecha);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Induccion induccion = (Induccion) o;
        return Objects.equals(id, induccion.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
