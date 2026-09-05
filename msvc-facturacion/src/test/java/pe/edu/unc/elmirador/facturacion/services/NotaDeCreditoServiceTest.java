package pe.edu.unc.elmirador.facturacion.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import pe.edu.unc.elmirador.facturacion.dto.request.EmitirNotaDeCreditoRequest;
import pe.edu.unc.elmirador.facturacion.models.entity.Factura;
import pe.edu.unc.elmirador.facturacion.models.entity.LineaDeFactura;
import pe.edu.unc.elmirador.facturacion.models.entity.NotaDeCredito;
import pe.edu.unc.elmirador.facturacion.models.vo.ConceptoFacturable;
import pe.edu.unc.elmirador.facturacion.models.vo.Conformidad;
import pe.edu.unc.elmirador.facturacion.models.vo.Detraccion;
import pe.edu.unc.elmirador.facturacion.models.vo.Dinero;
import pe.edu.unc.elmirador.facturacion.models.vo.MotivoDeAjuste;
import pe.edu.unc.elmirador.facturacion.models.vo.NumeroDeComprobante;
import pe.edu.unc.elmirador.facturacion.models.vo.SnapshotComercial;
import pe.edu.unc.elmirador.facturacion.repositories.FacturaRepository;
import pe.edu.unc.elmirador.facturacion.repositories.NotaDeCreditoRepository;

class NotaDeCreditoServiceTest {

    private NotaDeCreditoRepository notaRepository;
    private FacturaRepository facturaRepository;
    private NotaDeCreditoService servicio;

    @BeforeEach
    void preparar() {
        notaRepository = mock(NotaDeCreditoRepository.class);
        facturaRepository = mock(FacturaRepository.class);
        Clock reloj = Clock.fixed(OffsetDateTime.parse("2026-03-10T10:00:00Z").toInstant(), ZoneId.of("America/Lima"));
        servicio = new NotaDeCreditoService(notaRepository, facturaRepository, reloj);
        when(notaRepository.save(any(NotaDeCredito.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("emitir nota de credito")
    void emitirNC() {
        SnapshotComercial snap = new SnapshotComercial("ord-1", "cli-1", new Dinero(new BigDecimal("100.00"), "PEN"), "PEN", OffsetDateTime.now());
        Detraccion det = Detraccion.sinDetraccion("PEN");
        Factura factura = Factura.abrir("f-1", snap, det);
        factura.agregarLinea(new LineaDeFactura("l-1", "ord-1", ConceptoFacturable.FLETE, "", new Dinero(new BigDecimal("100.00"), "PEN")));
        factura.registrarConformidad(Conformidad.conforme(OffsetDateTime.now()));
        factura.emitir(NumeroDeComprobante.de("F001", 1), OffsetDateTime.now());
        
        when(facturaRepository.findById("f-1")).thenReturn(Optional.of(factura));

        var req = new EmitirNotaDeCreditoRequest("f-1", MotivoDeAjuste.ERROR_DE_FACTURACION, new BigDecimal("10.00"), "error");
        var res = servicio.emitir(req);

        assertThat(res.montoMonto()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(factura.saldoAjustable().monto()).isEqualByComparingTo(new BigDecimal("90.00"));
    }
}
