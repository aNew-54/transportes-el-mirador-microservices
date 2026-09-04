package pe.edu.unc.elmirador.unidades.models.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProgramaDeMantenimientoTest {

    @Test
    @DisplayName("Borde: requiereAlerta a 501 km del proximo servicio retorna false")
    void requiereAlertaA501KmDelProximoServicioRetornaFalse() {
        // Proximo servicio en 10 000 km. A 501 km faltantes el km actual es 9 499 km.
        ProgramaDeMantenimiento programa = new ProgramaDeMantenimiento(
                new Kilometraje(0),
                new Kilometraje(10_000),
                IntervaloDeMantenimiento.ACEITE_Y_FILTROS);

        boolean alerta = programa.requiereAlerta(new Kilometraje(9_499));

        assertThat(alerta).isFalse();
    }

    @Test
    @DisplayName("Borde: requiereAlerta a 500 km del proximo servicio retorna true")
    void requiereAlertaA500KmDelProximoServicioRetornaTrue() {
        // Proximo servicio en 10 000 km. A 500 km faltantes el km actual es 9 500 km.
        ProgramaDeMantenimiento programa = new ProgramaDeMantenimiento(
                new Kilometraje(0),
                new Kilometraje(10_000),
                IntervaloDeMantenimiento.ACEITE_Y_FILTROS);

        boolean alerta = programa.requiereAlerta(new Kilometraje(9_500));

        assertThat(alerta).isTrue();
    }

    @Test
    @DisplayName("Borde: requiereAlerta a 499 km del proximo servicio retorna true")
    void requiereAlertaA499KmDelProximoServicioRetornaTrue() {
        // Proximo servicio en 10 000 km. A 499 km faltantes el km actual es 9 501 km.
        ProgramaDeMantenimiento programa = new ProgramaDeMantenimiento(
                new Kilometraje(0),
                new Kilometraje(10_000),
                IntervaloDeMantenimiento.ACEITE_Y_FILTROS);

        boolean alerta = programa.requiereAlerta(new Kilometraje(9_501));

        assertThat(alerta).isTrue();
    }

    @Test
    @DisplayName("estaVencido retorna true cuando el kilometraje alcanza o excede el proximo servicio")
    void estaVencidoCuandoKilometrajeAlcanzaOExcedeProximoServicio() {
        ProgramaDeMantenimiento programa = new ProgramaDeMantenimiento(
                new Kilometraje(10_000),
                new Kilometraje(20_000),
                IntervaloDeMantenimiento.ACEITE_Y_FILTROS);

        assertThat(programa.estaVencido(new Kilometraje(19_999))).isFalse();
        assertThat(programa.estaVencido(new Kilometraje(20_000))).isTrue();
        assertThat(programa.estaVencido(new Kilometraje(20_001))).isTrue();
    }

    @Test
    @DisplayName("Fabrica 'of' calcula proximo servicio sumando los kilometros del intervalo")
    void fabricaOfCalculaProximoServicioSegunIntervalo() {
        ProgramaDeMantenimiento programa = ProgramaDeMantenimiento.of(
                new Kilometraje(15_000), IntervaloDeMantenimiento.REVISION_MAYOR);

        assertThat(programa.kmUltimoServicio().valor()).isEqualTo(15_000);
        assertThat(programa.kmProximoServicio().valor()).isEqualTo(35_000);
        assertThat(programa.intervalo()).isEqualTo(IntervaloDeMantenimiento.REVISION_MAYOR);
    }

    @Test
    @DisplayName("Crear programa con proximo servicio menor al ultimo servicio lanza IllegalArgumentException")
    void crearConProximoServicioMenorAUltimoServicioLanzaExcepcion() {
        assertThatThrownBy(() -> new ProgramaDeMantenimiento(
                new Kilometraje(20_000),
                new Kilometraje(10_000),
                IntervaloDeMantenimiento.ACEITE_Y_FILTROS))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
