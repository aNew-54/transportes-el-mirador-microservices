package pe.edu.unc.elmirador.facturacion.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import pe.edu.unc.elmirador.facturacion.clients.CobranzaGateway;
import pe.edu.unc.elmirador.facturacion.clients.ComercialGateway;
import pe.edu.unc.elmirador.facturacion.dto.request.AbrirFacturaRequest;
import pe.edu.unc.elmirador.facturacion.dto.request.DetraccionRequest;
import pe.edu.unc.elmirador.facturacion.dto.request.SnapshotComercialRequest;
import pe.edu.unc.elmirador.facturacion.exceptions.ConflictoDeRecursoException;
import pe.edu.unc.elmirador.facturacion.models.entity.Factura;
import pe.edu.unc.elmirador.facturacion.repositories.FacturaRepository;

class FacturaServiceTest {

    private FacturaRepository repositorio;
    private ComercialGateway comercialGateway;
    private CobranzaGateway cobranzaGateway;
    private FacturaService servicio;

    @BeforeEach
    void preparar() {
        repositorio = mock(FacturaRepository.class);
        comercialGateway = mock(ComercialGateway.class);
        cobranzaGateway = mock(CobranzaGateway.class);
        Clock reloj = Clock.fixed(OffsetDateTime.parse("2026-03-10T10:00:00Z").toInstant(), ZoneId.of("America/Lima"));
        servicio = new FacturaService(repositorio, comercialGateway, cobranzaGateway, reloj);
        when(repositorio.save(any(Factura.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("abrir factura")
    void abrir() {
        when(repositorio.existsByOrdenDeServicioId("ord-1")).thenReturn(false);
        when(comercialGateway.snapshotFacturableDe("ord-1")).thenReturn(
            new pe.edu.unc.elmirador.facturacion.models.vo.SnapshotComercial("ord-1", "cli-1", new pe.edu.unc.elmirador.facturacion.models.vo.Dinero(new BigDecimal("100"), "PEN"), "PEN", OffsetDateTime.now(), "CREDITO", 30)
        );
        var req = new AbrirFacturaRequest("ord-1", "cli-1", 
            new SnapshotComercialRequest(new BigDecimal("100"), "PEN", OffsetDateTime.now()),
            new DetraccionRequest(BigDecimal.ZERO, BigDecimal.ZERO, null));
        var res = servicio.abrir(req);
        assertThat(res.ordenDeServicioId()).isEqualTo("ord-1");
    }

    @Test
    @DisplayName("abrir factura repetida lanza ConflictoDeRecursoException")
    void abrirRepetida() {
        when(repositorio.existsByOrdenDeServicioId("ord-1")).thenReturn(true);
        var req = new AbrirFacturaRequest("ord-1", "cli-1", 
            new SnapshotComercialRequest(new BigDecimal("100"), "PEN", OffsetDateTime.now()),
            new DetraccionRequest(BigDecimal.ZERO, BigDecimal.ZERO, null));
        assertThatThrownBy(() -> servicio.abrir(req))
            .isInstanceOf(ConflictoDeRecursoException.class);
    }
}
