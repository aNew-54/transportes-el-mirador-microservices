package pe.edu.unc.elmirador.unidades.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.unidades.exceptions.KilometrajeDeAtencionInvalidoException;
import pe.edu.unc.elmirador.unidades.exceptions.OrdenCerradaException;
import pe.edu.unc.elmirador.unidades.models.entity.OrdenDeMantenimiento;
import pe.edu.unc.elmirador.unidades.models.entity.TrabajoRealizado;
import pe.edu.unc.elmirador.unidades.models.vo.Dinero;
import pe.edu.unc.elmirador.unidades.models.vo.EstadoDeOrden;
import pe.edu.unc.elmirador.unidades.models.vo.Kilometraje;
import pe.edu.unc.elmirador.unidades.models.vo.TipoDeMantenimiento;

class OrdenDeMantenimientoTest {

    private final LocalDate hoy = LocalDate.of(2026, 9, 4);

    // =========================================================================
    // INVARIANTE OMT-01
    // "Una orden cerrada es inmutable."
    // registrarTrabajo y cerrar sobre una orden CERRADA lanzan
    // =========================================================================

    @Test
    @DisplayName("OMT-01: registrarTrabajo sobre una orden CERRADA lanza OrdenCerradaException")
    void registrarTrabajoEnOrdenCerradaLanzaOrdenCerradaException_OMT01() {
        OrdenDeMantenimiento orden = OrdenDeMantenimiento.abrir(
                "OMT-001",
                "UNI-001",
                TipoDeMantenimiento.PREVENTIVO,
                new Kilometraje(10_000),
                new Kilometraje(5_000),
                hoy,
                "PEN");

        orden.cerrar(hoy);
        assertThat(orden.getEstado()).isEqualTo(EstadoDeOrden.CERRADA);

        TrabajoRealizado nuevoTrabajo = new TrabajoRealizado(
                "TR-1", "Cambio de filtro", new Dinero(new BigDecimal("120.00"), "PEN"));

        // Invariante OMT-01: orden cerrada es inmutable
        assertThatThrownBy(() -> orden.registrarTrabajo(nuevoTrabajo))
                .isInstanceOf(OrdenCerradaException.class)
                .hasMessageContaining("OMT-01");
    }

    @Test
    @DisplayName("OMT-01: cerrar una orden ya CERRADA lanza OrdenCerradaException (no idempotente)")
    void cerrarOrdenYaCerradaLanzaOrdenCerradaException_OMT01() {
        OrdenDeMantenimiento orden = OrdenDeMantenimiento.abrir(
                "OMT-001",
                "UNI-001",
                TipoDeMantenimiento.PREVENTIVO,
                new Kilometraje(10_000),
                new Kilometraje(5_000),
                hoy,
                "PEN");

        orden.cerrar(hoy);

        // Invariante OMT-01: intentar cerrar nuevamente lanza OrdenCerradaException
        assertThatThrownBy(() -> orden.cerrar(hoy.plusDays(1)))
                .isInstanceOf(OrdenCerradaException.class)
                .hasMessageContaining("OMT-01");
    }

    // =========================================================================
    // INVARIANTE OMT-02
    // "El kilometraje registrado no puede ser menor al del ultimo mantenimiento
    //  de la unidad."
    // abrir con kmAtencion menor al ultimo mantenimiento lanza; igual no lanza
    // =========================================================================

    @Test
    @DisplayName("OMT-02: abrir orden con kmAtencion menor al ultimo mantenimiento lanza KilometrajeDeAtencionInvalidoException")
    void abrirConKilometrajeMenorAlUltimoMantenimientoLanzaExcepcion_OMT02() {
        Kilometraje kmAtencionMenor = new Kilometraje(19_999);
        Kilometraje kmUltimoMantenimiento = new Kilometraje(20_000);

        // Invariante OMT-02
        assertThatThrownBy(() -> OrdenDeMantenimiento.abrir(
                "OMT-002",
                "UNI-001",
                TipoDeMantenimiento.PREVENTIVO,
                kmAtencionMenor,
                kmUltimoMantenimiento,
                hoy,
                "PEN"))
                .isInstanceOf(KilometrajeDeAtencionInvalidoException.class)
                .hasMessageContaining("OMT-02");
    }

    @Test
    @DisplayName("OMT-02: abrir orden con kmAtencion igual al ultimo mantenimiento no lanza excepcion")
    void abrirConKilometrajeIgualAlUltimoMantenimientoNoLanza_OMT02() {
        Kilometraje kmIgual = new Kilometraje(20_000);
        Kilometraje kmUltimoMantenimiento = new Kilometraje(20_000);

        OrdenDeMantenimiento orden = OrdenDeMantenimiento.abrir(
                "OMT-002",
                "UNI-001",
                TipoDeMantenimiento.PREVENTIVO,
                kmIgual,
                kmUltimoMantenimiento,
                hoy,
                "PEN");

        assertThat(orden.getKmAtencion().valor()).isEqualTo(20_000);
    }

    @Test
    @DisplayName("OMT-02: abrir orden con kmAtencion mayor al ultimo mantenimiento no lanza excepcion")
    void abrirConKilometrajeMayorAlUltimoMantenimientoNoLanza_OMT02() {
        Kilometraje kmMayor = new Kilometraje(25_000);
        Kilometraje kmUltimoMantenimiento = new Kilometraje(20_000);

        OrdenDeMantenimiento orden = OrdenDeMantenimiento.abrir(
                "OMT-002",
                "UNI-001",
                TipoDeMantenimiento.CORRECTIVO,
                kmMayor,
                kmUltimoMantenimiento,
                hoy,
                "PEN");

        assertThat(orden.getKmAtencion().valor()).isEqualTo(25_000);
    }

    // =========================================================================
    // Costo total y colecciones
    // =========================================================================

    @Test
    @DisplayName("costoTotal con lista vacia retorna cero en la moneda de la orden")
    void costoTotalConListaVaciaRetornaCero() {
        OrdenDeMantenimiento orden = OrdenDeMantenimiento.abrir(
                "OMT-003",
                "UNI-001",
                TipoDeMantenimiento.PREVENTIVO,
                new Kilometraje(10_000),
                new Kilometraje(5_000),
                hoy,
                "PEN");

        Dinero total = orden.costoTotal();

        assertThat(total.monto().compareTo(BigDecimal.ZERO)).isZero();
        assertThat(total.codigoMoneda()).isEqualTo("PEN");
    }

    @Test
    @DisplayName("costoTotal suma los Dinero de los trabajos registrados")
    void costoTotalSumaTrabajosRegistrados() {
        OrdenDeMantenimiento orden = OrdenDeMantenimiento.abrir(
                "OMT-003",
                "UNI-001",
                TipoDeMantenimiento.PREVENTIVO,
                new Kilometraje(10_000),
                new Kilometraje(5_000),
                hoy,
                "PEN");

        orden.registrarTrabajo(new TrabajoRealizado(
                "TR-1", "Cambio de aceite", new Dinero(new BigDecimal("150.00"), "PEN")));
        orden.registrarTrabajo(new TrabajoRealizado(
                "TR-2", "Cambio de filtros", new Dinero(new BigDecimal("80.50"), "PEN")));

        Dinero total = orden.costoTotal();

        assertThat(total.monto().compareTo(new BigDecimal("230.50"))).isZero();
        assertThat(total.codigoMoneda()).isEqualTo("PEN");
    }

    @Test
    @DisplayName("getTrabajos devuelve una copia inmutable de la lista")
    void getTrabajosDevuelveCopiaInmutable() {
        OrdenDeMantenimiento orden = OrdenDeMantenimiento.abrir(
                "OMT-004",
                "UNI-001",
                TipoDeMantenimiento.PREVENTIVO,
                new Kilometraje(10_000),
                new Kilometraje(5_000),
                hoy,
                "PEN");

        TrabajoRealizado trabajo = new TrabajoRealizado(
                "TR-1", "Mano de obra", new Dinero(new BigDecimal("50.00"), "PEN"));
        orden.registrarTrabajo(trabajo);

        assertThatThrownBy(() -> orden.getTrabajos().add(trabajo))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
