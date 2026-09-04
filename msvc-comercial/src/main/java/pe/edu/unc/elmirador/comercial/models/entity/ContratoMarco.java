package pe.edu.unc.elmirador.comercial.models.entity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import pe.edu.unc.elmirador.comercial.models.vo.ClausulaDeConsolidacion;
import pe.edu.unc.elmirador.comercial.models.vo.Dinero;
import pe.edu.unc.elmirador.comercial.models.vo.PeriodoDeVigencia;
import pe.edu.unc.elmirador.comercial.models.vo.Ruta;
import pe.edu.unc.elmirador.comercial.models.vo.TiempoLibre;
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeUnidad;

/**
 * Raiz del agregado ContratoMarco.
 * Sostiene las invariantes CTM-01 y CTM-02.
 */
public class ContratoMarco {

    private final String id;
    private final String clienteId;
    private final PeriodoDeVigencia vigencia;
    private final TiempoLibre tiempoLibre;
    private final ClausulaDeConsolidacion clausulaDeConsolidacion;
    private final List<TarifaPactada> tarifasPactadas;

    public ContratoMarco(
        String id,
        String clienteId,
        PeriodoDeVigencia vigencia,
        TiempoLibre tiempoLibre,
        ClausulaDeConsolidacion clausulaDeConsolidacion,
        List<TarifaPactada> tarifasPactadas
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id del contrato marco es obligatorio");
        }
        if (clienteId == null || clienteId.isBlank()) {
            throw new IllegalArgumentException("El clienteId es obligatorio");
        }
        if (vigencia == null) {
            throw new IllegalArgumentException("El periodo de vigencia es obligatorio");
        }
        if (tiempoLibre == null) {
            throw new IllegalArgumentException("El tiempo libre es obligatorio");
        }
        if (clausulaDeConsolidacion == null) {
            throw new IllegalArgumentException("La clausula de consolidacion es obligatoria");
        }
        if (tarifasPactadas == null) {
            throw new IllegalArgumentException("La lista de tarifas pactadas no puede ser nula");
        }
        this.id = id.trim();
        this.clienteId = clienteId.trim();
        this.vigencia = vigencia;
        this.tiempoLibre = tiempoLibre;
        this.clausulaDeConsolidacion = clausulaDeConsolidacion;
        this.tarifasPactadas = List.copyOf(tarifasPactadas);
    }

    public String id() {
        return id;
    }

    public String clienteId() {
        return clienteId;
    }

    public PeriodoDeVigencia vigencia() {
        return vigencia;
    }

    public TiempoLibre tiempoLibre() {
        return tiempoLibre;
    }

    public ClausulaDeConsolidacion clausulaDeConsolidacion() {
        return clausulaDeConsolidacion;
    }

    public List<TarifaPactada> tarifasPactadas() {
        return List.copyOf(tarifasPactadas);
    }

    /**
     * Invariante CTM-01:
     * Las tarifas pactadas solo aplican dentro del periodo de vigencia.
     * Devuelve Optional.empty() fuera de vigencia, aunque la tarifa exista.
     */
    public Optional<Dinero> tarifaPara(Ruta ruta, TipoDeUnidad tipoUnidad, LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha es obligatoria");
        }
        if (ruta == null) {
            throw new IllegalArgumentException("La ruta es obligatoria");
        }
        if (tipoUnidad == null) {
            throw new IllegalArgumentException("El tipo de unidad es obligatorio");
        }
        if (!this.vigencia.estaVigenteEn(fecha)) {
            return Optional.empty();
        }
        return this.tarifasPactadas.stream()
            .filter(t -> t.ruta().equals(ruta) && t.tipoUnidad() == tipoUnidad)
            .map(TarifaPactada::precio)
            .findFirst();
    }

    /**
     * Invariante CTM-02:
     * La clausula de consolidacion obliga a todas las ordenes del contrato marco uniformemente.
     */
    public boolean obligaAConsolidar() {
        return this.clausulaDeConsolidacion.permitida();
    }

    /**
     * Evalua la viabilidad de consolidacion cruzando la autorizacion general con restricciones especificas.
     */
    public boolean admiteConsolidacionDe(Ruta ruta) {
        if (ruta == null) {
            throw new IllegalArgumentException("La ruta es obligatoria");
        }
        if (!this.clausulaDeConsolidacion.permitida()) {
            return false;
        }
        // Cada restriccion nombra un corredor excluido, sin mas. La version anterior buscaba los
        // prefijos "NO_" y "EXCLUYE_" dentro del texto: un protocolo inventado, no documentado en
        // ningun contrato, que codificaba una regla de negocio en cadenas de texto libre.
        for (String corredorExcluido : this.clausulaDeConsolidacion.restricciones()) {
            if (corredorExcluido.equalsIgnoreCase(ruta.corredor())) {
                return false;
            }
        }
        return true;
    }
}
