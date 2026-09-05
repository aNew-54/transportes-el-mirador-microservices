package pe.edu.unc.elmirador.ejecucion.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.RetryableException;
import pe.edu.unc.elmirador.ejecucion.clients.dto.HojaDeRutaRemota;
import pe.edu.unc.elmirador.ejecucion.exceptions.ProgramacionIntegrationException;

class ProgramacionGatewayTest {

    private ProgramacionClient cliente;
    private ProgramacionGateway pasarela;

    @BeforeEach
    void setUp() {
        cliente = mock(ProgramacionClient.class);
        pasarela = new ProgramacionGateway(cliente);
    }

    private static Request peticionFalsa() {
        return Request.create(Request.HttpMethod.GET, "/internal/v1/viajes/VIA-1/hoja-de-ruta",
                java.util.Map.of(), null, new RequestTemplate());
    }

    @Test
    void devuelveLaRespuestaEnCasoDeExito() {
        when(cliente.obtenerHojaDeRuta("VIA-1")).thenReturn(new HojaDeRutaRemota(
                "VIA-1", "DESPACHADO", "UNI-1", List.of(), "Obs", List.of()));

        HojaDeRutaRemota hoja = pasarela.obtenerHojaDeRuta("VIA-1");

        assertThat(hoja.viajeId()).isEqualTo("VIA-1");
        assertThat(hoja.estado()).isEqualTo("DESPACHADO");
    }

    @Test
    void unErrorDelServidorRemotoEsUnFalloDeIntegracion() {
        when(cliente.obtenerHojaDeRuta("VIA-1"))
                .thenThrow(new FeignException.InternalServerError("boom", peticionFalsa(), null, null));

        assertThatThrownBy(() -> pasarela.obtenerHojaDeRuta("VIA-1"))
                .isInstanceOf(ProgramacionIntegrationException.class)
                .hasMessageContaining("VIA-1");
    }

    @Test
    void unCuatrocientosNueveRemotoNoEsUnErrorDeDominioPropio() {
        when(cliente.obtenerHojaDeRuta("VIA-1"))
                .thenThrow(new FeignException.Conflict("conflict", peticionFalsa(), null, null));

        assertThatThrownBy(() -> pasarela.obtenerHojaDeRuta("VIA-1"))
                .isInstanceOf(ProgramacionIntegrationException.class);
    }

    @Test
    void unCuatrocientosCuatroRemotoNoSeConvierteEnUnCuatrocientosCuatroPropio() {
        when(cliente.obtenerHojaDeRuta("VIA-1"))
                .thenThrow(new FeignException.NotFound("no esta", peticionFalsa(), null, null));

        assertThatThrownBy(() -> pasarela.obtenerHojaDeRuta("VIA-1"))
                .isInstanceOf(ProgramacionIntegrationException.class);
    }

    @Test
    void siElProveedorNoContestaTambienEsUnFalloDeIntegracion() {
        when(cliente.obtenerHojaDeRuta("VIA-1")).thenThrow(new RetryableException(
                -1, "Connection refused", Request.HttpMethod.GET, (Long) null, peticionFalsa()));

        assertThatThrownBy(() -> pasarela.obtenerHojaDeRuta("VIA-1"))
                .isInstanceOf(ProgramacionIntegrationException.class)
                .hasMessageContaining("no respondio");
    }

    @Test
    void unCuerpoIncompletoEsUnFalloDeIntegracion() {
        when(cliente.obtenerHojaDeRuta("VIA-1")).thenReturn(new HojaDeRutaRemota(
                null, null, null, null, null, null));

        assertThatThrownBy(() -> pasarela.obtenerHojaDeRuta("VIA-1"))
                .isInstanceOf(ProgramacionIntegrationException.class)
                .hasMessageContaining("incompleta");
    }
}
