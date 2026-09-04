package pe.edu.unc.elmirador.conductores.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.conductores.exceptions.HorasExcedidasException;
import pe.edu.unc.elmirador.conductores.models.entity.Conductor;
import pe.edu.unc.elmirador.conductores.models.vo.CategoriaDeLicencia;
import pe.edu.unc.elmirador.conductores.models.vo.EstadoDeHabilitacion;
import pe.edu.unc.elmirador.conductores.models.vo.HorasDeConduccion;
import pe.edu.unc.elmirador.conductores.models.vo.NumeroDeLicencia;
import pe.edu.unc.elmirador.conductores.models.vo.PeriodoDeVigencia;
import pe.edu.unc.elmirador.conductores.models.vo.TipoDeUnidad;

/**
 * La ventana de computo dura veinticuatro horas, no cuarenta y ocho.
 *
 * <p>PeriodoDeVigencia.estaVigenteEn es inclusivo en los dos extremos, asi que usarlo sobre una
 * ventana [fecha, fecha+1] hace que el dia siguiente siga contando como el mismo periodo: un
 * conductor que agoto sus horas el lunes seguiria sin horas el martes, y en el contrato 3 aparece
 * como no elegible sin motivo comprensible.
 */
class VentanaDeConduccionTest {

    private final LocalDate lunes = LocalDate.of(2026, 9, 7);

    private Conductor conductorConHoras(BigDecimal horas, LocalDate dia) {
        return new Conductor(
                "CON-050",
                "Elena Quiroz",
                new NumeroDeLicencia("Q87654321"),
                CategoriaDeLicencia.A_IIIC,
                new PeriodoDeVigencia(lunes.minusYears(1), lunes.plusYears(1)),
                new HorasDeConduccion(horas, new PeriodoDeVigencia(dia, dia.plusDays(1))),
                EstadoDeHabilitacion.habilitado(),
                List.of());
    }

    @Test
    @DisplayName("CON-02: el conductor que agoto sus horas el lunes vuelve a tenerlas el martes")
    void laVentanaDuraUnDiaNoDos() {
        Conductor conductor = conductorConHoras(new BigDecimal("10.00"), lunes);
        LocalDate martes = lunes.plusDays(1);

        assertThat(conductor.estaHabilitadoPara(lunes, TipoDeUnidad.FURGON, new BigDecimal("1.00"), null))
                .as("[CON-02] el lunes ya no le quedan horas")
                .isFalse();

        conductor.acumularHoras(new BigDecimal("3.00"), martes);

        assertThat(conductor.getHorasAcumuladas().horas())
                .as("[CON-02] el martes abre ventana nueva: 0 + 3")
                .isEqualByComparingTo(new BigDecimal("3.00"));
        assertThat(conductor.getHorasAcumuladas().ventanaDeComputo().desde()).isEqualTo(martes);
        assertThat(conductor.estaHabilitadoPara(martes, TipoDeUnidad.FURGON, new BigDecimal("7.00"), null))
                .isTrue();
    }

    @Test
    @DisplayName("CON-02: dentro del mismo dia las horas se suman, no se reinician")
    void dentroDelMismoDiaAcumula() {
        Conductor conductor = conductorConHoras(new BigDecimal("4.00"), lunes);

        conductor.acumularHoras(new BigDecimal("3.00"), lunes);

        assertThat(conductor.getHorasAcumuladas().horas()).isEqualByComparingTo(new BigDecimal("7.00"));
        assertThatThrownBy(() -> conductor.acumularHoras(new BigDecimal("3.01"), lunes))
                .as("[CON-02] 7.00 + 3.01 supera el maximo normado de 10.00")
                .isInstanceOf(HorasExcedidasException.class);
        assertThat(conductor.getHorasAcumuladas().horas())
                .as("[CON-02] el intento fallido no altera el acumulado")
                .isEqualByComparingTo(new BigDecimal("7.00"));
    }

    @Test
    @DisplayName("cubre() distingue el dia de la ventana de cualquier otro")
    void cubreSoloElDiaDeLaVentana() {
        HorasDeConduccion horas = HorasDeConduccion.ventanaDe(lunes);

        assertThat(horas.cubre(lunes)).isTrue();
        assertThat(horas.cubre(lunes.plusDays(1))).isFalse();
        assertThat(horas.cubre(lunes.minusDays(1))).isFalse();
        assertThatThrownBy(() -> horas.cubre(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
