package pe.edu.unc.elmirador.unidades.models.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;
import pe.edu.unc.elmirador.unidades.models.vo.PeriodoDeVigencia;
import pe.edu.unc.elmirador.unidades.models.vo.TipoDeDocumento;

@Entity
@Table(name = "documentos_vehiculares")
public class DocumentoVehicular {

    @Id
    @Column(name = "id", length = 80, nullable = false)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", length = 30, nullable = false)
    private TipoDeDocumento tipo;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "desde", column = @Column(name = "vigente_desde", nullable = false)),
        @AttributeOverride(name = "hasta", column = @Column(name = "vigente_hasta", nullable = false))
    })
    private PeriodoDeVigencia vigencia;

    @Column(name = "numero", length = 50)
    private String numero;

    /** Exigido por JPA. No usar: no valida ninguna invariante. */
    protected DocumentoVehicular() {
    }

    public DocumentoVehicular(String id, TipoDeDocumento tipo, PeriodoDeVigencia vigencia, String numero) {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de documento no puede ser nulo");
        }
        if (vigencia == null) {
            throw new IllegalArgumentException("La vigencia del documento no puede ser nula");
        }
        this.id = id;
        this.tipo = tipo;
        this.vigencia = vigencia;
        this.numero = numero;
    }

    public boolean estaVigente(LocalDate fecha) {
        if (fecha == null || vigencia == null) {
            return false;
        }
        return vigencia.estaVigenteEn(fecha);
    }

    public String getId() {
        return id;
    }

    public TipoDeDocumento getTipo() {
        return tipo;
    }

    public PeriodoDeVigencia getVigencia() {
        return vigencia;
    }

    public String getNumero() {
        return numero;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentoVehicular that = (DocumentoVehicular) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
