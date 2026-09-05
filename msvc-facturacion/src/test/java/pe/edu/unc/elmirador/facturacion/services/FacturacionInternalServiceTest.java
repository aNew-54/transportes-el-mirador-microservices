package pe.edu.unc.elmirador.facturacion.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import pe.edu.unc.elmirador.facturacion.dto.internal.request.ConceptoFacturableRequest;
import pe.edu.unc.elmirador.facturacion.dto.internal.request.RegistrarConformidadRequest;
import pe.edu.unc.elmirador.facturacion.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.facturacion.models.vo.ConceptoFacturable;
import pe.edu.unc.elmirador.facturacion.models.vo.EstadoDeConformidad;
import pe.edu.unc.elmirador.facturacion.models.entity.Factura;
import pe.edu.unc.elmirador.facturacion.models.entity.PeticionIdempotente;
import pe.edu.unc.elmirador.facturacion.models.vo.Detraccion;
import pe.edu.unc.elmirador.facturacion.models.vo.Dinero;
import pe.edu.unc.elmirador.facturacion.models.vo.EstadoDeFactura;
import pe.edu.unc.elmirador.facturacion.models.vo.SnapshotComercial;
import pe.edu.unc.elmirador.facturacion.repositories.FacturaRepository;
import pe.edu.unc.elmirador.facturacion.repositories.PeticionIdempotenteRepository;

class FacturacionInternalServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 9, 10);
    private static final OffsetDateTime FIRMA = OffsetDateTime.of(2026, 9, 10, 15, 20, 0, 0, ZoneOffset.of("-05:00"));

    private FacturaRepository repositorio;
    private PeticionIdempotenteRepository idempotencia;
    private FacturacionInternalService servicio;

    @BeforeEach
    void preparar() {
        repositorio = mock(FacturaRepository.class);
        idempotencia = mock(PeticionIdempotenteRepository.class);
        Clock relojFijo = Clock.fixed(
                HOY.atStartOfDay(ZoneId.of("America/Lima")).toInstant(), ZoneId.of("America/Lima"));
        servicio = new FacturacionInternalService(repositorio, idempotencia, relojFijo);
        when(repositorio.save(any(Factura.class))).thenAnswer(inv -> inv.getArgument(0));
        when(idempotencia.findById(any())).thenReturn(Optional.empty());
    }

    private Factura facturaBloqueada() {
        SnapshotComercial snapshot = new SnapshotComercial(
                "ORD-2026-000123", "CLI-0007", new Dinero(new BigDecimal("1821.60"), "PEN"), "PEN", FIRMA, "CREDITO", 30);
        Detraccion detraccion = new Detraccion(
                new BigDecimal("4.00"), new Dinero(new BigDecimal("72.86"), "PEN"), "00-123-456789");
        return Factura.abrir("FAC-2026-000310", snapshot, detraccion);
    }

    @Test
    @DisplayName("la de idempotencia: el mismo POST dos veces con la misma clave devuelve el resultado original y el agregado se toco una vez")
    void idempotencia() {
        Factura f = facturaBloqueada();
        when(repositorio.findByOrdenDeServicioId("ORD-2026-000123")).thenReturn(Optional.of(f));

        String clave = "VIA-2026-00045:ORD-2026-000123:conformidad";
        var peticion = new RegistrarConformidadRequest(
                "VIA-2026-00045", "ORD-2026-000123", EstadoDeConformidad.FIRMADA, FIRMA,
                List.of(new ConceptoFacturableRequest(ConceptoFacturable.ESTIBA, new BigDecimal("180.00"), "PEN", null)),
                List.of()
        );

        var primera = servicio.registrarConformidad(clave, peticion);
        assertThat(primera.repetida()).isFalse();
        assertThat(primera.cuerpo().facturaId()).isEqualTo("FAC-2026-000310");
        assertThat(f.lineas()).hasSize(1);
        assertThat(f.conformidad().registrada()).isTrue();

        when(idempotencia.findById(clave)).thenReturn(Optional.of(
                new PeticionIdempotente(clave, "FAC-2026-000310", FIRMA)
        ));

        var segunda = servicio.registrarConformidad(clave, peticion);
        assertThat(segunda.repetida()).isTrue();
        assertThat(segunda.cuerpo().facturaId()).isEqualTo("FAC-2026-000310");

        assertThat(f.lineas()).hasSize(1);
        verify(repositorio, times(1)).save(any(Factura.class));
        verify(idempotencia, times(1)).save(any(PeticionIdempotente.class));
    }

    @Test
    @DisplayName("la de incidenciasSinResolver con contenido: la factura queda BLOQUEADA (FAC-05)")
    void incidenciasBloqueanLaFactura() {
        Factura f = facturaBloqueada();
        when(repositorio.findByOrdenDeServicioId("ORD-2026-000123")).thenReturn(Optional.of(f));

        var peticion = new RegistrarConformidadRequest(
                "VIA-2026-00045", "ORD-2026-000123", EstadoDeConformidad.FIRMADA, FIRMA,
                List.of(new ConceptoFacturableRequest(ConceptoFacturable.ESTIBA, new BigDecimal("180.00"), "PEN", null)),
                List.of("Falta sello")
        );

        servicio.registrarConformidad("clave-2", peticion);

        assertThat(f.conformidad().incidenciasSinResolver()).containsExactly("Falta sello");
        assertThat(f.estado()).isEqualTo(EstadoDeFactura.BLOQUEADA);
    }

    @Test
    @DisplayName("la de una orden sin factura abierta: 404")
    void ordenSinFactura() {
        when(repositorio.findByOrdenDeServicioId("ORD-999")).thenReturn(Optional.empty());

        var peticion = new RegistrarConformidadRequest(
                "VIA-1", "ORD-999", EstadoDeConformidad.FIRMADA, FIRMA, List.of(), List.of()
        );

        assertThatThrownBy(() -> servicio.registrarConformidad("clave", peticion))
                .isInstanceOf(RecursoNoEncontradoException.class);
        
        verify(repositorio, never()).save(any());
    }
}
