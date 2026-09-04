package pe.edu.unc.elmirador.comercial.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.comercial.exceptions.CotizacionVencidaException;
import pe.edu.unc.elmirador.comercial.exceptions.DominioComercialException;
import pe.edu.unc.elmirador.comercial.models.entity.Cotizacion;
import pe.edu.unc.elmirador.comercial.models.vo.Carga;
import pe.edu.unc.elmirador.comercial.models.vo.Dinero;
import pe.edu.unc.elmirador.comercial.models.vo.EstadoDeCotizacion;
import pe.edu.unc.elmirador.comercial.models.vo.MotivoDeRechazo;
import pe.edu.unc.elmirador.comercial.models.vo.PeriodoDeVigencia;
import pe.edu.unc.elmirador.comercial.models.vo.Ruta;
import pe.edu.unc.elmirador.comercial.models.vo.Tarifa;
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeCarga;

class CotizacionTest {

    private final String cotizacionId = "COT-2026-001";
    private final String clienteId = "CLI-0007";
    private final String tarifarioId = "TAR-2026-01";
    private final Carga carga = new Carga(5000, new BigDecimal("15.5"), TipoDeCarga.PALETIZADA);
    private final Ruta ruta = new Ruta("Cajamarca", "Trujillo", "COSTA_NORTE");
    private final Tarifa tarifa = new Tarifa(Dinero.de("1200.00", "PEN"), List.of(), null);
    private final LocalDate emision = LocalDate.of(2026, 9, 1);
    // Periodo de 7 dias calendario inclusivo: 2026-09-01 al 2026-09-07
    private final PeriodoDeVigencia vigenciaSieteDias = PeriodoDeVigencia.de(emision, 7);

    @Test
    @DisplayName("COT-01: Aceptar cotizacion en el ultimo dia de vigencia (borde exacto D5) tiene exito")
    void cot01_aceptarEnUltimoDiaDeVigenciaTieneExito() {
        Cotizacion cotizacion = Cotizacion.emitir(
            cotizacionId, clienteId, tarifarioId, carga, ruta, tarifa, vigenciaSieteDias
        );

        LocalDate ultimoDia = LocalDate.of(2026, 9, 7);
        cotizacion.aceptar(ultimoDia);

        assertThat(cotizacion.estado()).isEqualTo(EstadoDeCotizacion.ACEPTADA);
    }

    @Test
    @DisplayName("COT-01: Aceptar cotizacion un dia despues de la vigencia viola la invariante y lanza CotizacionVencidaException")
    void cot01_aceptarCotizacionVencidaLanzaExcepcion() {
        Cotizacion cotizacion = Cotizacion.emitir(
            cotizacionId, clienteId, tarifarioId, carga, ruta, tarifa, vigenciaSieteDias
        );

        LocalDate diaDespues = LocalDate.of(2026, 9, 8);
        assertThatThrownBy(() -> cotizacion.aceptar(diaDespues))
            .isInstanceOf(CotizacionVencidaException.class)
            .hasMessageContaining("ha vencido");

        assertThat(cotizacion.estado()).isEqualTo(EstadoDeCotizacion.VENCIDA);
    }

    @Test
    @DisplayName("COT-01: Aceptar una cotizacion ya ACEPTADA lanza excepcion de dominio comercial")
    void cot01_aceptarCotizacionYaAceptadaLanzaExcepcion() {
        Cotizacion cotizacion = Cotizacion.emitir(
            cotizacionId, clienteId, tarifarioId, carga, ruta, tarifa, vigenciaSieteDias
        );
        cotizacion.aceptar(emision);

        assertThatThrownBy(() -> cotizacion.aceptar(emision.plusDays(1)))
            .isInstanceOf(DominioComercialException.class)
            .hasMessageContaining("Solo se puede aceptar una cotizacion en estado EMITIDA");
    }

    @Test
    @DisplayName("COT-01: Aceptar una cotizacion ya RECHAZADA lanza excepcion de dominio comercial")
    void cot01_aceptarCotizacionRechazadaLanzaExcepcion() {
        Cotizacion cotizacion = Cotizacion.emitir(
            cotizacionId, clienteId, tarifarioId, carga, ruta, tarifa, vigenciaSieteDias
        );
        cotizacion.rechazar(MotivoDeRechazo.PRECIO, emision);

        assertThatThrownBy(() -> cotizacion.aceptar(emision.plusDays(1)))
            .isInstanceOf(DominioComercialException.class)
            .hasMessageContaining("Solo se puede aceptar una cotizacion en estado EMITIDA");
    }

    @Test
    @DisplayName("Borde obligatorio: Cotizacion con vigencia de 6 dias lanza IllegalArgumentException en la fabrica emitir")
    void debeRechazarCotizacionConVigenciaDeSeisDias() {
        PeriodoDeVigencia vigenciaSeisDias = PeriodoDeVigencia.de(emision, 6);

        assertThatThrownBy(() -> Cotizacion.emitir(
            cotizacionId, clienteId, tarifarioId, carga, ruta, tarifa, vigenciaSeisDias
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("exactamente 7 dias calendario");
    }

    @Test
    @DisplayName("Borde obligatorio: Cotizacion con vigencia de 8 dias lanza IllegalArgumentException en la fabrica emitir")
    void debeRechazarCotizacionConVigenciaDeOchoDias() {
        PeriodoDeVigencia vigenciaOchoDias = PeriodoDeVigencia.de(emision, 8);

        assertThatThrownBy(() -> Cotizacion.emitir(
            cotizacionId, clienteId, tarifarioId, carga, ruta, tarifa, vigenciaOchoDias
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("exactamente 7 dias calendario");
    }

    @Test
    @DisplayName("Rechazar cotizacion valida registra motivo y cambia estado a RECHAZADA")
    void debeRechazarCotizacionConMotivo() {
        Cotizacion cotizacion = Cotizacion.emitir(
            cotizacionId, clienteId, tarifarioId, carga, ruta, tarifa, vigenciaSieteDias
        );

        cotizacion.rechazar(MotivoDeRechazo.PLAZO, emision.plusDays(2));

        assertThat(cotizacion.estado()).isEqualTo(EstadoDeCotizacion.RECHAZADA);
        assertThat(cotizacion.motivoDeRechazo()).isEqualTo(MotivoDeRechazo.PLAZO);
    }

    @Test
    @DisplayName("Operaciones en Cotizacion con fecha nula lanzan IllegalArgumentException (regla D1)")
    void debeRechazarFechasNulasEnCotizacion() {
        Cotizacion cotizacion = Cotizacion.emitir(
            cotizacionId, clienteId, tarifarioId, carga, ruta, tarifa, vigenciaSieteDias
        );

        assertThatThrownBy(() -> cotizacion.aceptar(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fecha es obligatoria");

        assertThatThrownBy(() -> cotizacion.rechazar(MotivoDeRechazo.PRECIO, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fecha es obligatoria");

        assertThatThrownBy(() -> cotizacion.haVencidoEn(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fecha es obligatoria");
    }
}
