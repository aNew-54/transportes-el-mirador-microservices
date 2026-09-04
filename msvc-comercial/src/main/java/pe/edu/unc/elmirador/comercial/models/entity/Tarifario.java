package pe.edu.unc.elmirador.comercial.models.entity;

import java.time.LocalDate;
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
public class Tarifario {

    private final String id;
    private final PeriodoDeVigencia vigencia;
    private final List<PrecioDeTarifario> precios;
    private final List<Recargo> recargosEstandar;

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
        this.precios = List.copyOf(precios);
        this.recargosEstandar = List.copyOf(recargosEstandar);
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
