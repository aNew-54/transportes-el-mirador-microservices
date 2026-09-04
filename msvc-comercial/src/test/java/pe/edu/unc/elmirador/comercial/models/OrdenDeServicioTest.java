package pe.edu.unc.elmirador.comercial.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.comercial.exceptions.CondicionDePagoInconsistenteException;
import pe.edu.unc.elmirador.comercial.exceptions.ReajusteRequeridoException;
import pe.edu.unc.elmirador.comercial.exceptions.TransicionDeOrdenInvalidaException;
import pe.edu.unc.elmirador.comercial.models.entity.OrdenDeServicio;
import pe.edu.unc.elmirador.comercial.models.vo.Carga;
import pe.edu.unc.elmirador.comercial.models.vo.CondicionDePago;
import pe.edu.unc.elmirador.comercial.models.vo.Dinero;
import pe.edu.unc.elmirador.comercial.models.vo.EstadoCrediticio;
import pe.edu.unc.elmirador.comercial.models.vo.EstadoDeOrden;
import pe.edu.unc.elmirador.comercial.models.vo.Ruta;
import pe.edu.unc.elmirador.comercial.models.vo.Tarifa;
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeCarga;

class OrdenDeServicioTest {

    private final String ordenId = "ORD-2026-000123";
    private final String clienteId = "CLI-0007";
    private final Carga cargaInicial = new Carga(6000, new BigDecimal("18.0"), TipoDeCarga.GENERAL);
    private final Carga cargaReajustada = new Carga(8000, new BigDecimal("22.5"), TipoDeCarga.GENERAL);
    private final Ruta ruta = new Ruta("Cajamarca", "Trujillo", "COSTA_NORTE");
    private final Tarifa tarifaBase = new Tarifa(Dinero.de("1000.00", "PEN"), List.of(), null);
    private final LocalDate hoy = LocalDate.of(2026, 9, 10);

    @Test
    @DisplayName("ORD-02: Orden a CREDITO para cliente SUSPENDIDO viola invariante y lanza CondicionDePagoInconsistenteException")
    void ord02_ordenACreditoConClienteSuspendidoLanzaExcepcion() {
        EstadoCrediticio suspendido = EstadoCrediticio.suspendido(hoy.minusDays(5));
        CondicionDePago credito30 = CondicionDePago.credito(30);

        assertThatThrownBy(() -> OrdenDeServicio.crear(
            ordenId, clienteId, null, cargaInicial, ruta, tarifaBase, credito30, suspendido
        ))
            .isInstanceOf(CondicionDePagoInconsistenteException.class)
            .hasMessageContaining("CREDITO")
            .hasMessageContaining("SUSPENDIDO");
    }

    @Test
    @DisplayName("ORD-02: La misma orden al CONTADO para un cliente SUSPENDIDO si se crea con exito")
    void ord02_ordenAlContadoConClienteSuspendidoSeCrea() {
        EstadoCrediticio suspendido = EstadoCrediticio.suspendido(hoy.minusDays(5));
        CondicionDePago contado = CondicionDePago.contado();

        OrdenDeServicio orden = OrdenDeServicio.crear(
            ordenId, clienteId, null, cargaInicial, ruta, tarifaBase, contado, suspendido
        );

        assertThat(orden.estado()).isEqualTo(EstadoDeOrden.BORRADOR);
        assertThat(orden.condicionDePago().esAlContado()).isTrue();
    }

    @Test
    @DisplayName("ORD-02: EstadoCrediticio nulo en fabrica crear lanza IllegalArgumentException (regla D2, no se asume favorable)")
    void ord02_estadoCrediticioNuloLanzaExcepcion() {
        CondicionDePago credito30 = CondicionDePago.credito(30);

        assertThatThrownBy(() -> OrdenDeServicio.crear(
            ordenId, clienteId, null, cargaInicial, ruta, tarifaBase, credito30, null
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("estado crediticio es obligatorio");
    }

    @Test
    @DisplayName("ORD-01: reajustarCarga sobre orden PROGRAMADA sin importe viola la invariante y lanza ReajusteRequeridoException")
    void ord01_reajusteSinImporteEnOrdenProgramadaLanzaExcepcion() {
        EstadoCrediticio vigente = EstadoCrediticio.vigente(hoy);
        OrdenDeServicio orden = OrdenDeServicio.crear(
            ordenId, clienteId, null, cargaInicial, ruta, tarifaBase, CondicionDePago.contado(), vigente
        );
        orden.confirmar();
        orden.marcarProgramada();

        assertThatThrownBy(() -> orden.reajustarCarga(cargaReajustada, null))
            .isInstanceOf(ReajusteRequeridoException.class)
            .hasMessageContaining("no admite cambio de carga sin generar reajuste");

        assertThatThrownBy(() -> orden.reajustarCarga(cargaReajustada, Dinero.cero("PEN")))
            .isInstanceOf(ReajusteRequeridoException.class);
    }

    @Test
    @DisplayName("ORD-01: reajustarCarga sobre orden PROGRAMADA con importe valido anade el recargo a la tarifa")
    void ord01_reajusteConImporteEnOrdenProgramadaActualizaTarifa() {
        EstadoCrediticio vigente = EstadoCrediticio.vigente(hoy);
        OrdenDeServicio orden = OrdenDeServicio.crear(
            ordenId, clienteId, null, cargaInicial, ruta, tarifaBase, CondicionDePago.contado(), vigente
        );
        orden.confirmar();
        orden.marcarProgramada();

        Dinero importeReajuste = Dinero.de("200.00", "PEN");
        orden.reajustarCarga(cargaReajustada, importeReajuste);

        assertThat(orden.carga()).isEqualTo(cargaReajustada);
        // Base 1000.00 + recargo 200.00 (20%) = 1200.00 PEN
        assertThat(orden.tarifa().total()).isEqualTo(Dinero.de("1200.00", "PEN"));
    }

    @Test
    @DisplayName("ORD-01: reajustarCarga sobre orden en BORRADOR no exige importe y cambia la carga sin mas")
    void ord01_reajusteEnBorradorNoExigeImporte() {
        EstadoCrediticio vigente = EstadoCrediticio.vigente(hoy);
        OrdenDeServicio orden = OrdenDeServicio.crear(
            ordenId, clienteId, null, cargaInicial, ruta, tarifaBase, CondicionDePago.contado(), vigente
        );

        orden.reajustarCarga(cargaReajustada, null);

        assertThat(orden.carga()).isEqualTo(cargaReajustada);
        assertThat(orden.tarifa().total()).isEqualTo(Dinero.de("1000.00", "PEN"));
    }

    @Test
    @DisplayName("Confirmar orden en estado no BORRADOR lanza TransicionDeOrdenInvalidaException")
    void debeRechazarConfirmarOrdenEnEstadoInvalido() {
        EstadoCrediticio vigente = EstadoCrediticio.vigente(hoy);
        OrdenDeServicio orden = OrdenDeServicio.crear(
            ordenId, clienteId, null, cargaInicial, ruta, tarifaBase, CondicionDePago.contado(), vigente
        );
        orden.confirmar();

        assertThatThrownBy(orden::confirmar)
            .isInstanceOf(TransicionDeOrdenInvalidaException.class)
            .hasMessageContaining("Solo se puede confirmar una orden en estado BORRADOR");
    }

    @Test
    @DisplayName("Cancelar antes del despacho cancela sin generar falso flete")
    void debeCancelarAntesDelDespachoSinFalsoFlete() {
        EstadoCrediticio vigente = EstadoCrediticio.vigente(hoy);
        OrdenDeServicio orden = OrdenDeServicio.crear(
            ordenId, clienteId, null, cargaInicial, ruta, tarifaBase, CondicionDePago.contado(), vigente
        );
        orden.confirmar();

        orden.cancelar(hoy, "CLIENTE_SOLICITA");

        assertThat(orden.estado()).isEqualTo(EstadoDeOrden.CANCELADA);
        assertThat(orden.falsoFlete()).isNull();
    }

    @Test
    @DisplayName("Borde obligatorio: Cancelar tras despacho genera falso flete por exactamente la mitad de la tarifa y exige autorizacion")
    void debeGenerarFalsoFletePorLaMitadTrasDespacho() {
        EstadoCrediticio vigente = EstadoCrediticio.vigente(hoy);
        OrdenDeServicio orden = OrdenDeServicio.crear(
            ordenId, clienteId, null, cargaInicial, ruta, tarifaBase, CondicionDePago.contado(), vigente
        );
        orden.confirmar();
        orden.marcarProgramada();
        orden.marcarDespachada();

        // Sin autorizacion de gerencia debe lanzar IllegalArgumentException
        assertThatThrownBy(() -> orden.cancelar(hoy, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("autorizacion de gerencia registrada");

        assertThatThrownBy(() -> orden.cancelar(hoy, "   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("autorizacion de gerencia registrada");

        orden.cancelar(hoy, "GERENCIA_OPERACIONES");

        assertThat(orden.estado()).isEqualTo(EstadoDeOrden.CANCELADA);
        assertThat(orden.falsoFlete()).isNotNull();
        // Tarifa total 1000.00 PEN -> falso flete = 500.00 PEN
        assertThat(orden.falsoFlete().total()).isEqualTo(Dinero.de("500.00", "PEN"));
        assertThat(orden.canceladoPor()).isEqualTo("GERENCIA_OPERACIONES");
    }

    @Test
    @DisplayName("Cancelar orden con fecha nula lanza IllegalArgumentException (regla D1)")
    void debeRechazarCancelacionConFechaNula() {
        EstadoCrediticio vigente = EstadoCrediticio.vigente(hoy);
        OrdenDeServicio orden = OrdenDeServicio.crear(
            ordenId, clienteId, null, cargaInicial, ruta, tarifaBase, CondicionDePago.contado(), vigente
        );

        assertThatThrownBy(() -> orden.cancelar(null, "GERENCIA"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fecha de cancelacion es obligatoria");
    }
}
