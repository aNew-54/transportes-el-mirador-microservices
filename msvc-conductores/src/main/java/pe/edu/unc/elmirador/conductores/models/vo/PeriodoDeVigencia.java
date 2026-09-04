package pe.edu.unc.elmirador.conductores.models.vo;

import jakarta.persistence.Embeddable;

import java.time.LocalDate;

@Embeddable
public record PeriodoDeVigencia(LocalDate desde, LocalDate hasta) {

    public PeriodoDeVigencia {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin del periodo no pueden ser nulas");
        }
        if (!hasta.isAfter(desde)) {
            throw new IllegalArgumentException("La fecha 'hasta' debe ser posterior a la fecha 'desde'");
        }
    }

    public boolean estaVigenteEn(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha de referencia no puede ser nula");
        }
        return !fecha.isBefore(desde) && !fecha.isAfter(hasta);
    }

    public boolean venceDentroDe(int dias, LocalDate ref) {
        if (ref == null) {
            throw new IllegalArgumentException("La fecha de referencia no puede ser nula");
        }
        if (dias < 0) {
            throw new IllegalArgumentException("El numero de dias no puede ser negativo");
        }
        return estaVigenteEn(ref) && !hasta.isAfter(ref.plusDays(dias));
    }
}
