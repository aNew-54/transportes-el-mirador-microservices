package pe.edu.unc.elmirador.unidades.models.vo;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public record ProgramaDeMantenimiento(
        Kilometraje kmUltimoServicio,
        Kilometraje kmProximoServicio,
        @Enumerated(EnumType.STRING)
        IntervaloDeMantenimiento intervalo) {

    public ProgramaDeMantenimiento {
        if (kmUltimoServicio == null || kmProximoServicio == null || intervalo == null) {
            throw new IllegalArgumentException("Los parametros del programa de mantenimiento no pueden ser nulos");
        }
        if (kmProximoServicio.valor() < kmUltimoServicio.valor()) {
            throw new IllegalArgumentException("El proximo servicio no puede ser menor al ultimo servicio");
        }
    }

    public static ProgramaDeMantenimiento of(Kilometraje kmUltimoServicio, IntervaloDeMantenimiento intervalo) {
        if (kmUltimoServicio == null || intervalo == null) {
            throw new IllegalArgumentException("Los parametros no pueden ser nulos");
        }
        Kilometraje kmProximo = new Kilometraje(kmUltimoServicio.valor() + intervalo.kilometros());
        return new ProgramaDeMantenimiento(kmUltimoServicio, kmProximo, intervalo);
    }

    public boolean estaVencido(Kilometraje km) {
        if (km == null) {
            return false;
        }
        return km.valor() >= kmProximoServicio.valor();
    }

    public boolean requiereAlerta(Kilometraje km) {
        if (km == null) {
            return false;
        }
        return km.valor() >= (kmProximoServicio.valor() - 500);
    }
}
