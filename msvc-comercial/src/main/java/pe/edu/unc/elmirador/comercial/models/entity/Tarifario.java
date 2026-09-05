package pe.edu.unc.elmirador.comercial.models.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import pe.edu.unc.elmirador.comercial.exceptions.TarifarioVigenteDuplicadoException;
import pe.edu.unc.elmirador.comercial.models.vo.Dinero;
import pe.edu.unc.elmirador.comercial.models.vo.PeriodoDeVigencia;
import pe.edu.unc.elmirador.comercial.models.vo.Recargo;
import pe.edu.unc.elmirador.comercial.models.vo.Ruta;
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeUnidad;

/**
 * Raiz del agregado Tarifario.
 * Sostiene la invariante TAR-01.
 */
@Entity
@Table(name = "tarifarios")
public class Tarifario {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "desde", column = @Column(name = "vigencia_desde", nullable = false)),
        @AttributeOverride(name = "hasta", column = @Column(name = "vigencia_hasta", nullable = false))
    })
    private PeriodoDeVigencia vigencia;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "tarifario_id", nullable = false)
    private List<PrecioDeTarifario> precios = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "tarifario_recargos",
        joinColumns = @JoinColumn(name = "tarifario_id", nullable = false)
    )
    private List<Recargo> recargosEstandar = new ArrayList<>();

    /** Exigido por JPA. No usar: no valida ninguna invariante. */
    protected Tarifario() {
    }

    public Tarifario(
        String id,
        PeriodoDeVigencia vigencia,
        List<PrecioDeTarifario> precios,
        List<Recargo> recargosEstandar
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id del tarifario es obligatorio");
        }
        if (vigencia == null) {
            throw new IllegalArgumentException("El periodo de vigencia es obligatorio");
        }
        if (precios == null) {
            throw new IllegalArgumentException("La lista de precios no puede ser nula");
        }
        if (recargosEstandar == null) {
            throw new IllegalArgumentException("La lista de recargos estandar no puede ser nula");
        }
        this.id = id.trim();
        this.vigencia = vigencia;
        this.precios.addAll(precios);
        this.recargosEstandar.addAll(recargosEstandar);
    }

    public String id() {
        return id;
    }

    public PeriodoDeVigencia vigencia() {
        return vigencia;
    }

    public List<PrecioDeTarifario> precios() {
        return List.copyOf(precios);
    }

    public List<Recargo> recargosEstandar() {
        return List.copyOf(recargosEstandar);
    }

    public Optional<Dinero> tarifaPara(Ruta ruta, TipoDeUnidad tipoUnidad) {
        if (ruta == null) {
            throw new IllegalArgumentException("La ruta es obligatoria");
        }
        if (tipoUnidad == null) {
            throw new IllegalArgumentException("El tipo de unidad es obligatorio");
        }
        return this.precios.stream()
            .filter(p -> p.ruta().equals(ruta) && p.tipoUnidad() == tipoUnidad)
            .map(PrecioDeTarifario::precio)
            .findFirst();
    }

    public boolean estaVigenteEn(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha es obligatoria");
        }
        return this.vigencia.estaVigenteEn(fecha);
    }

    /**
     * Invariante TAR-01:
     * Solo un tarifario puede estar vigente a la vez. Si los periodos de vigencia se solapan,
     * lanza TarifarioVigenteDuplicadoException.
     */
    public void sucedeA(Tarifario anterior) {
        if (anterior == null) {
            throw new IllegalArgumentException("El tarifario anterior es obligatorio");
        }
        if (this.vigencia.seSolapaCon(anterior.vigencia())) {
            throw new TarifarioVigenteDuplicadoException(
                "El nuevo tarifario (" + this.vigencia.desde() + " a " + this.vigencia.hasta()
                    + ") se solapa con el tarifario anterior (" + anterior.vigencia().desde()
                    + " a " + anterior.vigencia().hasta() + ")"
            );
        }
    }
}
