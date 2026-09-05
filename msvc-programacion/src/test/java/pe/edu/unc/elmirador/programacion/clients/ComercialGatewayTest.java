package pe.edu.unc.elmirador.programacion.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.RetryableException;
import pe.edu.unc.elmirador.programacion.clients.dto.CargaRemota;
import pe.edu.unc.elmirador.programacion.clients.dto.OrdenRemota;
import pe.edu.unc.elmirador.programacion.clients.dto.RutaRemota;
import pe.edu.unc.elmirador.programacion.clients.dto.VentanaRemota;
import pe.edu.unc.elmirador.programacion.exceptions.ComercialIntegrationException;
import pe.edu.unc.elmirador.programacion.models.vo.TipoDeCarga;

class ComercialGatewayTest {

    private ComercialClient cliente;
    private ComercialGateway pasarela;

    @BeforeEach
    void setUp() {
        cliente = mock(ComercialClient.class);
        pasarela = new ComercialGateway(cliente);
    }

    private static Request peticionFalsa() {
        return Request.create(Request.HttpMethod.GET, "/internal/v1/ordenes/ord-1",
                java.util.Map.of(), null, new RequestTemplate());
    }

    @Test
    void traduceLaRespuestaDelContratoAlObjetoDeValorPropio() {
        OrdenRemota remota = new OrdenRemota(
                "ORD-1", "CLI-1", "CONFIRMADA",
                new CargaRemota(8500, new BigDecimal("24.5"), "PALLETS", "ALIMENTARIA"),
                new RutaRemota("Cajamarca", "Trujillo", "COSTA_NORTE", 296),
                new VentanaRemota(OffsetDateTime.parse("2026-09-10T06:00:00-05:00"), OffsetDateTime.parse("2026-09-10T18:00:00-05:00")),
                true, List.of("SOLO_CARGA_ALIMENTARIA"), "FURGON"
        );
        when(cliente.obtenerOrden("ORD-1")).thenReturn(remota);

        OrdenConfirmada orden = pasarela.obtenerOrden("ORD-1");

        assertThat(orden.ordenId()).isEqualTo("ORD-1");
        assertThat(orden.carga().tipo()).isEqualTo(TipoDeCarga.PALETIZADA);
        assertThat(orden.ruta().corredor()).isEqualTo("COSTA_NORTE");
        assertThat(orden.clausula().permitida()).isTrue();
    }

    @Test
    void unErrorDelServidorRemotoEsUnFalloDeIntegracion() {
        when(cliente.obtenerOrden("ORD-1"))
                .thenThrow(new FeignException.InternalServerError("boom", peticionFalsa(), null, null));

        assertThatThrownBy(() -> pasarela.obtenerOrden("ORD-1"))
                .isInstanceOf(ComercialIntegrationException.class);
    }

    @Test
    void unCuatrocientosCuatroRemotoNoSeConvierteEnUnCuatrocientosCuatroPropio() {
        when(cliente.obtenerOrden("ORD-1"))
                .thenThrow(new FeignException.NotFound("no esta", peticionFalsa(), null, null));

        assertThatThrownBy(() -> pasarela.obtenerOrden("ORD-1"))
                .isInstanceOf(ComercialIntegrationException.class);
    }

    @Test
    void siComercialNoContestaTambienEsUnFalloDeIntegracion() {
        when(cliente.obtenerOrden("ORD-1")).thenThrow(new RetryableException(
                -1, "Connection refused", Request.HttpMethod.GET, (Long) null, peticionFalsa()));

        assertThatThrownBy(() -> pasarela.obtenerOrden("ORD-1"))
                .isInstanceOf(ComercialIntegrationException.class);
    }

    @Test
    void unCuerpoIncompletoEsUnFalloDeIntegracion() {
        when(cliente.obtenerOrden("ORD-1")).thenReturn(new OrdenRemota(
                "ORD-1", "CLI-1", null, null, null, null, true, List.of(), "FURGON"));

        assertThatThrownBy(() -> pasarela.obtenerOrden("ORD-1"))
                .isInstanceOf(ComercialIntegrationException.class)
                .hasMessageContaining("incompleta");
    }
}
