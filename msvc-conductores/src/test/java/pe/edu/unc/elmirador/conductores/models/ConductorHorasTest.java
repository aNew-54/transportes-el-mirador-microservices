package pe.edu.unc.elmirador.conductores.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.conductores.exceptions.HorasExcedidasException;
import pe.edu.unc.elmirador.conductores.models.entity.Conductor;
import pe.edu.unc.elmirador.conductores.models.vo.CategoriaDeLicencia;
import pe.edu.unc.elmirador.conductores.models.vo.EstadoDeHabilitacion;
import pe.edu.unc.elmirador.conductores.models.vo.HorasDeConduccion;
import pe.edu.unc.elmirador.conductores.models.vo.MotivoDeNoElegibilidad;
import pe.edu.unc.elmirador.conductores.models.vo.NumeroDeLicencia;
import pe.edu.unc.elmirador.conductores.models.vo.PeriodoDeVigencia;
import pe.edu.unc.elmirador.conductores.models.vo.TipoDeUnidad;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConductorHorasTest {

    private final LocalDate hoy = LocalDate.of(2026, 9, 10);
    private Conductor conductor;

    @BeforeEach
    void setUp() {
        PeriodoDeVigencia vigenciaLicencia = new PeriodoDeVigencia(
                hoy.minusYears(1),
                hoy.plusYears(2)
        );
        PeriodoDeVigencia ventanaInicial = new PeriodoDeVigencia(hoy, hoy.plusDays(1));
        HorasDeConduccion horasIniciales = new HorasDeConduccion(new BigDecimal("4.00"), ventanaInicial);

        conductor = new Conductor(
                "CON-001",
                "Carlos Mendoza",
                new NumeroDeLicencia("Q12345678"),
                CategoriaDeLicencia.A_IIIC,
                vigenciaLicencia,
                horasIniciales,
                EstadoDeHabilitacion.habilitado(),
                Collections.emptyList()
        );
    }

    @Test
    @DisplayName("CON-02: acumularHoras en el borde exacto del maximo normado no lanza y actualiza horas")
    void acumularHorasEnBordeExactoNoLanza_CON02() {
        // Conductor tiene 4.00 horas. Agregamos 6.00 para alcanzar exactamente 10.00 horas
        conductor.acumularHoras(new BigDecimal("6.00"), hoy);

        assertThat(conductor.getHorasAcumuladas().horas())
                .as("[CON-02] Las horas acumuladas deben ser exactamente 10.00")
                .isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("CON-02: acumularHoras que cruzaria el maximo lanza HorasExcedidasException y NO altera el acumulado")
    void acumularHorasQueCruzaElMaximoLanzaHorasExcedidasExceptionYNoAlteraAcumulado_CON02() {
        BigDecimal horasPrevias = conductor.getHorasAcumuladas().horas();

        // 4.00 + 6.01 = 10.01 > 10.00
        assertThatThrownBy(() -> conductor.acumularHoras(new BigDecimal("6.01"), hoy))
                .as("[CON-02] Acumular horas por encima del maximo de 10.00 debe lanzar HorasExcedidasException")
                .isInstanceOf(HorasExcedidasException.class)
                .hasMessageContaining("superarian el maximo normado");

        // El acumulado debe mantenerse inalterado
        assertThat(conductor.getHorasAcumuladas().horas())
                .as("[CON-02] El fallo no debe alterar las horas acumuladas previamente")
                .isEqualByComparingTo(horasPrevias);
    }

    @Test
    @DisplayName("CON-02: Conductor sin horas disponibles no es elegible y reporta HORAS_INSUFICIENTES")
    void conductorSinHorasDisponiblesNoEsElegible_CON02() {
        // Llevar al conductor al maximo de 10.00 horas
        conductor.acumularHoras(new BigDecimal("6.00"), hoy);

        // Se solicitan 2.00 horas pero dispone de 0.00
        List<String> motivos = conductor.motivosDeNoElegibilidad(
                hoy,
                TipoDeUnidad.FURGON,
                new BigDecimal("2.00"),
                null
        );

        assertThat(conductor.estaHabilitadoPara(hoy, TipoDeUnidad.FURGON, new BigDecimal("2.00"), null))
                .as("[CON-02] Conductor con horas agotadas no debe estar habilitado")
                .isFalse();
        assertThat(motivos)
                .as("[CON-02] Debe incluir el motivo HORAS_INSUFICIENTES")
                .contains(MotivoDeNoElegibilidad.HORAS_INSUFICIENTES.codigo());
    }

    @Test
    @DisplayName("CON-02: registrarDescanso libera horas y vuelve a hacer elegible a un conductor que no lo era")
    void registrarDescansoLiberaHorasYVuelveHabilitadoAlConductor_CON02() {
        // Agotar horas acumuladas
        conductor.acumularHoras(new BigDecimal("6.00"), hoy);
        assertThat(conductor.estaHabilitadoPara(hoy, TipoDeUnidad.FURGON, new BigDecimal("2.00"), null)).isFalse();

        // Registrar descanso
        conductor.registrarDescanso(hoy);

        // Verifica que el acumulado vuelve a 0.00
        assertThat(conductor.getHorasAcumuladas().horas())
                .as("[CON-02] El descanso debe reiniciar las horas acumuladas a 0.00")
                .isEqualByComparingTo("0.00");

        // Vuelve a ser elegible
        assertThat(conductor.estaHabilitadoPara(hoy, TipoDeUnidad.FURGON, new BigDecimal("2.00"), null))
                .as("[CON-02] Tras el descanso el conductor vuelve a estar habilitado")
                .isTrue();
        assertThat(conductor.motivosDeNoElegibilidad(hoy, TipoDeUnidad.FURGON, new BigDecimal("2.00"), null))
                .isEmpty();
    }

    @Test
    @DisplayName("CON-02: acumularHoras con fecha fuera de la ventana renueva la ventana y reinicia el acumulado en vez de lanzar")
    void acumularHorasConFechaFueraDeVentanaRenuevaVentanaYReiniciaAcumulado_CON02() {
        // En la ventana actual tiene 4.00 horas acumuladas
        assertThat(conductor.getHorasAcumuladas().horas()).isEqualByComparingTo("4.00");

        // La ventana es de un dia: el dia siguiente ya esta fuera
        LocalDate fechaNueva = hoy.plusDays(1);
        conductor.acumularHoras(new BigDecimal("5.00"), fechaNueva);

        // La ventana se renovo y el acumulado parte de 0 + 5.00 = 5.00
        assertThat(conductor.getHorasAcumuladas().horas())
                .as("[CON-02] Al caer fuera de la ventana, reinicia el acumulado y registra las horas nuevas")
                .isEqualByComparingTo("5.00");
        assertThat(conductor.getHorasAcumuladas().ventanaDeComputo().desde())
                .isEqualTo(fechaNueva);
    }

    @Test
    @DisplayName("acumularHoras con fecha nula lanza IllegalArgumentException (el dominio no lee el reloj)")
    void acumularHorasConFechaNulaLanzaIllegalArgumentException() {
        assertThatThrownBy(() -> conductor.acumularHoras(new BigDecimal("2.00"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no puede ser nula");
    }

    @Test
    @DisplayName("registrarDescanso con fecha nula lanza IllegalArgumentException (el dominio no lee el reloj)")
    void registrarDescansoConFechaNulaLanzaIllegalArgumentException() {
        assertThatThrownBy(() -> conductor.registrarDescanso(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no puede ser nula");
    }

    @Test
    @DisplayName("acumularHoras con horas nulas o negativas lanza IllegalArgumentException")
    void acumularHorasConHorasNulasONegativasLanzaIllegalArgumentException() {
        assertThatThrownBy(() -> conductor.acumularHoras(null, hoy))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> conductor.acumularHoras(new BigDecimal("-1.00"), hoy))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
