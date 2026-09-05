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
import pe.edu.unc.elmirador.programacion.clients.dto.CapacidadRemota;
import pe.edu.unc.elmirador.programacion.clients.dto.ElegibilidadUnidadRemota;
import pe.edu.unc.elmirador.programacion.exceptions.UnidadesIntegrationException;

class UnidadesGatewayTest {

    private UnidadesClient cliente;
    private UnidadesGateway pasarela;

    @BeforeEach
    void setUp() {
        cliente = mock(UnidadesClient.class);
        pasarela = new UnidadesGateway(cliente);
    }

    private static Request peticionFalsa() {
        return Request.create(Request.HttpMethod.GET, "/internal/v1/unidades/uni-1/elegibilidad",
                java.util.Map.of(), null, new RequestTemplate());
    }
    
    private final OffsetDateTime desde = OffsetDateTime.parse("2026-09-10T06:00:00-05:00");
    private final OffsetDateTime hasta = OffsetDateTime.parse("2026-09-10T18:00:00-05:00");

    @Test
    void traduceLaRespuestaDelContratoAlObjetoDeValorPropio() {
        ElegibilidadUnidadRemota remota = new ElegibilidadUnidadRemota(
                "UNI-004", false, List.of("DOCUMENTO_VENCIDO:SOAT"),
                new CapacidadRemota(10000, new BigDecimal("32.0")),
                "FURGON", "INOPERATIVA"
        );
        when(cliente.consultarElegibilidad("UNI-004", desde, hasta, 1000, new BigDecimal("2.5"), "GENERAL"))
            .thenReturn(remota);

        EvaluacionDeUnidad eval = pasarela.consultarElegibilidad("UNI-004", desde, hasta, 1000, new BigDecimal("2.5"), "GENERAL");

        assertThat(eval.elegibilidad().elegible()).isFalse();
        assertThat(eval.elegibilidad().motivos()).containsExactly("DOCUMENTO_VENCIDO:SOAT");
        assertThat(eval.capacidad().pesoMaximoKg()).isEqualTo(10000);
        assertThat(eval.tipoUnidad()).isEqualTo("FURGON");
    }

    @Test
    void unErrorDelServidorRemotoEsUnFalloDeIntegracion() {
        when(cliente.consultarElegibilidad("UNI-004", desde, hasta, 1000, new BigDecimal("2.5"), "GENERAL"))
                .thenThrow(new FeignException.InternalServerError("boom", peticionFalsa(), null, null));

        assertThatThrownBy(() -> pasarela.consultarElegibilidad("UNI-004", desde, hasta, 1000, new BigDecimal("2.5"), "GENERAL"))
                .isInstanceOf(UnidadesIntegrationException.class);
    }

    @Test
    void unCuatrocientosCuatroRemotoNoSeConvierteEnUnCuatrocientosCuatroPropio() {
        when(cliente.consultarElegibilidad("UNI-004", desde, hasta, 1000, new BigDecimal("2.5"), "GENERAL"))
                .thenThrow(new FeignException.NotFound("no esta", peticionFalsa(), null, null));

        assertThatThrownBy(() -> pasarela.consultarElegibilidad("UNI-004", desde, hasta, 1000, new BigDecimal("2.5"), "GENERAL"))
                .isInstanceOf(UnidadesIntegrationException.class);
    }

    @Test
    void siUnidadesNoContestaTambienEsUnFalloDeIntegracion() {
        when(cliente.consultarElegibilidad("UNI-004", desde, hasta, 1000, new BigDecimal("2.5"), "GENERAL")).thenThrow(new RetryableException(
                -1, "Connection refused", Request.HttpMethod.GET, (Long) null, peticionFalsa()));

        assertThatThrownBy(() -> pasarela.consultarElegibilidad("UNI-004", desde, hasta, 1000, new BigDecimal("2.5"), "GENERAL"))
                .isInstanceOf(UnidadesIntegrationException.class);
    }

    @Test
    void unCuerpoIncompletoEsUnFalloDeIntegracion() {
        when(cliente.consultarElegibilidad("UNI-004", desde, hasta, 1000, new BigDecimal("2.5"), "GENERAL")).thenReturn(new ElegibilidadUnidadRemota(
                "UNI-004", true, null, null, null, null));

        assertThatThrownBy(() -> pasarela.consultarElegibilidad("UNI-004", desde, hasta, 1000, new BigDecimal("2.5"), "GENERAL"))
                .isInstanceOf(UnidadesIntegrationException.class)
                .hasMessageContaining("incompleta");
    }
}
