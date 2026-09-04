package pe.edu.unc.elmirador.comercial.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.comercial.models.entity.ContratoMarco;
import pe.edu.unc.elmirador.comercial.models.entity.TarifaPactada;
import pe.edu.unc.elmirador.comercial.models.vo.ClausulaDeConsolidacion;
import pe.edu.unc.elmirador.comercial.models.vo.Dinero;
import pe.edu.unc.elmirador.comercial.models.vo.PeriodoDeVigencia;
import pe.edu.unc.elmirador.comercial.models.vo.Ruta;
import pe.edu.unc.elmirador.comercial.models.vo.TiempoLibre;
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeUnidad;

class ContratoMarcoTest {

    private final String contratoId = "CTM-2026-001";
    private final String clienteId = "CLI-0007";
    // Vigencia de un ano: 2026-01-01 al 2026-12-31
    private final LocalDate desde = LocalDate.of(2026, 1, 1);
    private final LocalDate hasta = LocalDate.of(2026, 12, 31);
    private final PeriodoDeVigencia vigenciaAnual = PeriodoDeVigencia.de(desde, hasta);
    private final TiempoLibre tiempoLibre = new TiempoLibre(2);
    private final Ruta rutaNorte = new Ruta("Cajamarca", "Trujillo", "COSTA_NORTE");
    private final Dinero precioPactado = Dinero.de("1500.00", "PEN");
    private final TarifaPactada tarifaPactada = new TarifaPactada("TP-01", rutaNorte, TipoDeUnidad.FURGON, precioPactado);

    @Test
    @DisplayName("CTM-01: tarifaPara devuelve la tarifa pactada en el ultimo dia de vigencia")
    void ctm01_tarifaParaDevuelveValorEnUltimoDiaDeVigencia() {
        ContratoMarco contrato = new ContratoMarco(
            contratoId, clienteId, vigenciaAnual, tiempoLibre,
            ClausulaDeConsolidacion.permitida(List.of()),
            List.of(tarifaPactada)
        );

        Optional<Dinero> tarifa = contrato.tarifaPara(rutaNorte, TipoDeUnidad.FURGON, hasta);

        assertThat(tarifa).isPresent();
        assertThat(tarifa.get()).isEqualTo(precioPactado);
    }

    @Test
    @DisplayName("CTM-01: tarifaPara devuelve vacio un dia despues del fin de vigencia violando la vigencia")
    void ctm01_tarifaParaDevuelveVacioUnDiaDespuesDeFinDeVigencia() {
        ContratoMarco contrato = new ContratoMarco(
            contratoId, clienteId, vigenciaAnual, tiempoLibre,
            ClausulaDeConsolidacion.permitida(List.of()),
            List.of(tarifaPactada)
        );

        LocalDate diaDespues = hasta.plusDays(1);
        Optional<Dinero> tarifa = contrato.tarifaPara(rutaNorte, TipoDeUnidad.FURGON, diaDespues);

        assertThat(tarifa).isEmpty();
    }

    @Test
    @DisplayName("CTM-01: tarifaPara devuelve vacio antes del inicio de vigencia")
    void ctm01_tarifaParaDevuelveVacioAntesDeInicioDeVigencia() {
        ContratoMarco contrato = new ContratoMarco(
            contratoId, clienteId, vigenciaAnual, tiempoLibre,
            ClausulaDeConsolidacion.permitida(List.of()),
            List.of(tarifaPactada)
        );

        LocalDate diaAntes = desde.minusDays(1);
        Optional<Dinero> tarifa = contrato.tarifaPara(rutaNorte, TipoDeUnidad.FURGON, diaAntes);

        assertThat(tarifa).isEmpty();
    }

    @Test
    @DisplayName("CTM-02: Con clausula permitida = false, obligaAConsolidar() es false para todas las ordenes")
    void ctm02_clausulaNoPermitidaObligaAConsolidarFalseParaTodasLasOrdenes() {
        ContratoMarco contrato = new ContratoMarco(
            contratoId, clienteId, vigenciaAnual, tiempoLibre,
            ClausulaDeConsolidacion.noPermitida(),
            List.of(tarifaPactada)
        );

        assertThat(contrato.obligaAConsolidar()).isFalse();
        assertThat(contrato.admiteConsolidacionDe(rutaNorte)).isFalse();
    }

    @Test
    @DisplayName("CTM-02: Con clausula permitida = true, obligaAConsolidar() es true")
    void ctm02_clausulaPermitidaObligaAConsolidarTrue() {
        ContratoMarco contrato = new ContratoMarco(
            contratoId, clienteId, vigenciaAnual, tiempoLibre,
            ClausulaDeConsolidacion.permitida(List.of("SOLO_CARGA_ALIMENTARIA")),
            List.of(tarifaPactada)
        );

        assertThat(contrato.obligaAConsolidar()).isTrue();
        assertThat(contrato.admiteConsolidacionDe(rutaNorte)).isTrue();
    }

    @Test
    @DisplayName("admiteConsolidacionDe rechaza rutas cuyo corredor esta explicitamente excluido en las restricciones")
    void debeRechazarConsolidacionDeRutaExcluida() {
        ContratoMarco contrato = new ContratoMarco(
            contratoId, clienteId, vigenciaAnual, tiempoLibre,
            ClausulaDeConsolidacion.permitida(List.of("COSTA_NORTE")),
            List.of(tarifaPactada)
        );

        assertThat(contrato.obligaAConsolidar()).isTrue();
        assertThat(contrato.admiteConsolidacionDe(rutaNorte)).isFalse();
    }

    @Test
    @DisplayName("tarifaPara lanza IllegalArgumentException si la fecha es nula (regla D1)")
    void debeRechazarFechaNulaEnTarifaPara() {
        ContratoMarco contrato = new ContratoMarco(
            contratoId, clienteId, vigenciaAnual, tiempoLibre,
            ClausulaDeConsolidacion.permitida(List.of()),
            List.of(tarifaPactada)
        );

        assertThatThrownBy(() -> contrato.tarifaPara(rutaNorte, TipoDeUnidad.FURGON, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fecha es obligatoria");
    }
}
