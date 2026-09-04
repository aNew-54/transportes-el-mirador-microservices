package pe.edu.unc.elmirador.unidades.models.entity;

import java.time.LocalDate;
import pe.edu.unc.elmirador.unidades.models.vo.PeriodoDeVigencia;
import pe.edu.unc.elmirador.unidades.models.vo.TipoDeDocumento;

public class DocumentoVehicular {

    private final String id;
    private final TipoDeDocumento tipo;
    private final PeriodoDeVigencia vigencia;
    private final String numero;

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
}
