package pe.edu.unc.elmirador.unidades.models.vo;

import jakarta.persistence.Embeddable;
import java.time.LocalDate;

@Embeddable
public record PeriodoDeVigencia(LocalDate desde, LocalDate hasta) {

    public PeriodoDeVigencia {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Las fechas del periodo de vigencia no pueden ser nulas");
        }
        if (!hasta.isAfter(desde)) {
            throw new IllegalArgumentException(
                    "La fecha 'hasta' debe ser posterior a 'desde': desde=" + desde + ", hasta=" + hasta);
        }
    }

    public boolean estaVigenteEn(LocalDate fecha) {
        if (fecha == null) {
            return false;
        }
        return !fecha.isBefore(desde) && !fecha.isAfter(hasta);
    }

    public boolean venceDentroDe(int dias, LocalDate ref) {
        if (ref == null) {
            return false;
        }
        if (dias < 0) {
            throw new IllegalArgumentException("Los dias no pueden ser negativos: " + dias);
        }
        LocalDate limite = ref.plusDays(dias);
        return !hasta.isBefore(ref) && !hasta.isAfter(limite);
    }
}
