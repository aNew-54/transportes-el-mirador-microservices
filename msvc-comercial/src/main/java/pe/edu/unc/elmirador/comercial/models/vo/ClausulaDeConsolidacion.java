package pe.edu.unc.elmirador.comercial.models.vo;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Objeto de valor inmutable que expresa si la carga asociada puede consolidarse y bajo que restricciones.
 * Es lo que viaja en el contrato 1 y sostiene VIA-04.
 *
 * <p><strong>Es clase inmutable y no {@code record}</strong>: un objeto de valor que posee una
 * coleccion no puede ser un record en Hibernate.
 */
@Embeddable
public class ClausulaDeConsolidacion {

    @Column(name = "consolidacion_permitida", nullable = false)
    private boolean permitida;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "contrato_marco_restricciones",
        joinColumns = @JoinColumn(name = "contrato_marco_id", nullable = false)
    )
    @Column(name = "corredor", length = 100, nullable = false)
    private List<String> restricciones = new ArrayList<>();

    /** Exigido por JPA. No usar: no valida nada. */
    protected ClausulaDeConsolidacion() {
    }

    public ClausulaDeConsolidacion(boolean permitida, List<String> restricciones) {
        if (restricciones == null) {
            throw new IllegalArgumentException("La lista de restricciones no puede ser nula");
        }
        this.permitida = permitida;
        this.restricciones.addAll(restricciones);
    }

    /**
     * Clausula de una orden sin contrato marco: no hay pacto que prohiba consolidar, asi que se
     * permite sin restricciones. Tiene nombre a proposito —la alternativa era un `true` suelto dentro
     * del servicio de integracion— porque es una decision de negocio y Programacion la consume ciega.
     */
    public static ClausulaDeConsolidacion sinContratoMarco() {
        return new ClausulaDeConsolidacion(true, List.of());
    }

    public static ClausulaDeConsolidacion permitida(List<String> restricciones) {
        return new ClausulaDeConsolidacion(true, restricciones);
    }

    public static ClausulaDeConsolidacion noPermitida() {
        return new ClausulaDeConsolidacion(false, List.of());
    }

    public boolean permitida() {
        return permitida;
    }

    public List<String> restricciones() {
        return List.copyOf(restricciones);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClausulaDeConsolidacion otra)) {
            return false;
        }
        return permitida == otra.permitida
            && Objects.equals(restricciones, otra.restricciones);
    }

    @Override
    public int hashCode() {
        return Objects.hash(permitida, restricciones);
    }
}
