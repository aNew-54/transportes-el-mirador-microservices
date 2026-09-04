package pe.edu.unc.elmirador.comercial.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.comercial.exceptions.TarifarioVigenteDuplicadoException;
import pe.edu.unc.elmirador.comercial.models.entity.PrecioDeTarifario;
import pe.edu.unc.elmirador.comercial.models.entity.Tarifario;
import pe.edu.unc.elmirador.comercial.models.vo.Dinero;
import pe.edu.unc.elmirador.comercial.models.vo.PeriodoDeVigencia;
import pe.edu.unc.elmirador.comercial.models.vo.Recargo;
import pe.edu.unc.elmirador.comercial.models.vo.Ruta;
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeRecargo;
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeUnidad;

class TarifarioTest {

    private final Ruta rutaNorte = new Ruta("Cajamarca", "Trujillo", "COSTA_NORTE");
    private final Ruta rutaSur = new Ruta("Lima", "Arequipa", "PANAMERICANA_SUR");
    private final Dinero precioFurgon = Dinero.de("1800.00", "PEN");
    private final PrecioDeTarifario precioItem = new PrecioDeTarifario("PR-01", rutaNorte, TipoDeUnidad.FURGON, precioFurgon);
    private final Recargo recargoCombustible = new Recargo(TipoDeRecargo.COMBUSTIBLE, new BigDecimal("10"));

    @Test
    @DisplayName("TAR-01: Dos tarifarios con vigencias solapadas lanzan TarifarioVigenteDuplicadoException en sucedeA")
    void tar01_tarifariosConVigenciasSolapadasLanzanExcepcion() {
        PeriodoDeVigencia p1 = PeriodoDeVigencia.de(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
        PeriodoDeVigencia p2 = PeriodoDeVigencia.de(LocalDate.of(2026, 6, 15), LocalDate.of(2026, 12, 31));

        Tarifario t1 = new Tarifario("TAR-01", p1, List.of(precioItem), List.of(recargoCombustible));
        Tarifario t2 = new Tarifario("TAR-02", p2, List.of(precioItem), List.of(recargoCombustible));

        assertThatThrownBy(() -> t2.sucedeA(t1))
            .isInstanceOf(TarifarioVigenteDuplicadoException.class)
            .hasMessageContaining("se solapa con el tarifario anterior");
    }

    @Test
    @DisplayName("TAR-01: Dos tarifarios con vigencias consecutivas (sin solapamiento) se suceden sin lanzar")
    void tar01_tarifariosConsecutivosNoLanzanExcepcion() {
        PeriodoDeVigencia p1 = PeriodoDeVigencia.de(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
        PeriodoDeVigencia p2 = PeriodoDeVigencia.de(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31));

        Tarifario t1 = new Tarifario("TAR-01", p1, List.of(precioItem), List.of(recargoCombustible));
        Tarifario t2 = new Tarifario("TAR-02", p2, List.of(precioItem), List.of(recargoCombustible));

        t2.sucedeA(t1); // No lanza
    }

    @Test
    @DisplayName("tarifaPara devuelve el precio configurado para la ruta y tipo de unidad, o vacio si no existe")
    void debeConsultarTarifaParaRutaYUnidad() {
        PeriodoDeVigencia p = PeriodoDeVigencia.de(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        Tarifario tarifario = new Tarifario("TAR-01", p, List.of(precioItem), List.of(recargoCombustible));

        Optional<Dinero> encontrada = tarifario.tarifaPara(rutaNorte, TipoDeUnidad.FURGON);
        assertThat(encontrada).isPresent().contains(precioFurgon);

        Optional<Dinero> noEncontrada = tarifario.tarifaPara(rutaSur, TipoDeUnidad.FURGON);
        assertThat(noEncontrada).isEmpty();

        Optional<Dinero> tipoDistinto = tarifario.tarifaPara(rutaNorte, TipoDeUnidad.PLATAFORMA);
        assertThat(tipoDistinto).isEmpty();
    }

    @Test
    @DisplayName("estaVigenteEn responde segun la fecha y rechaza fecha nula (regla D1)")
    void debeValidarVigenciaTemporalYRechazarFechaNula() {
        PeriodoDeVigencia p = PeriodoDeVigencia.de(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
        Tarifario tarifario = new Tarifario("TAR-01", p, List.of(precioItem), List.of(recargoCombustible));

        assertThat(tarifario.estaVigenteEn(LocalDate.of(2026, 3, 15))).isTrue();
        assertThat(tarifario.estaVigenteEn(LocalDate.of(2026, 7, 1))).isFalse();

        assertThatThrownBy(() -> tarifario.estaVigenteEn(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fecha es obligatoria");
    }

    @Test
    @DisplayName("sucedeA rechaza tarifario anterior nulo")
    void debeRechazarTarifarioAnteriorNulo() {
        PeriodoDeVigencia p = PeriodoDeVigencia.de(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
        Tarifario tarifario = new Tarifario("TAR-01", p, List.of(precioItem), List.of(recargoCombustible));

        assertThatThrownBy(() -> tarifario.sucedeA(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tarifario anterior es obligatorio");
    }
}
