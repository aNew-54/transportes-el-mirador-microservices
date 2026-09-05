package pe.edu.unc.elmirador.programacion.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.RetryableException;
import pe.edu.unc.elmirador.programacion.clients.dto.ElegibilidadConductorRemota;
import pe.edu.unc.elmirador.programacion.exceptions.ConductoresIntegrationException;
import pe.edu.unc.elmirador.programacion.models.vo.ElegibilidadDeRecurso;

class ConductoresGatewayTest {

    private ConductoresClient cliente;
    private ConductoresGateway pasarela;

    @BeforeEach
    void setUp() {
        cliente = mock(ConductoresClient.class);
        pasarela = new ConductoresGateway(cliente);
    }

    private static Request peticionFalsa() {
        return Request.create(Request.HttpMethod.GET, "/internal/v1/conductores/con-1/elegibilidad",
                java.util.Map.of(), null, new RequestTemplate());
    }
    
    private final OffsetDateTime desde = OffsetDateTime.parse("2026-09-10T06:00:00-05:00");
    private final OffsetDateTime hasta = OffsetDateTime.parse("2026-09-10T18:00:00-05:00");

    @Test
    void traduceLaRespuestaDelContratoAlObjetoDeValorPropio() {
        ElegibilidadConductorRemota remota = new ElegibilidadConductorRemota(
                "CON-011", false, List.of("INDUCCION_VENCIDA:CLI-0019"),
                "A-IIIB", new BigDecimal("3.5")
        );
        when(cliente.consultarElegibilidad("CON-011", desde.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), hasta.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), "FURGON", "CLI-0019"))
            .thenReturn(remota);

        ElegibilidadDeRecurso eval = pasarela.consultarElegibilidad("CON-011", desde, hasta, "FURGON", "CLI-0019");

        assertThat(eval.elegible()).isFalse();
        assertThat(eval.motivos()).containsExactly("INDUCCION_VENCIDA:CLI-0019");
    }

    @Test
    void unErrorDelServidorRemotoEsUnFalloDeIntegracion() {
        when(cliente.consultarElegibilidad("CON-011", desde.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), hasta.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), "FURGON", "CLI-0019"))
                .thenThrow(new FeignException.InternalServerError("boom", peticionFalsa(), null, null));

        assertThatThrownBy(() -> pasarela.consultarElegibilidad("CON-011", desde, hasta, "FURGON", "CLI-0019"))
                .isInstanceOf(ConductoresIntegrationException.class);
    }

    @Test
    void unCuatrocientosCuatroRemotoNoSeConvierteEnUnCuatrocientosCuatroPropio() {
        when(cliente.consultarElegibilidad("CON-011", desde.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), hasta.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), "FURGON", "CLI-0019"))
                .thenThrow(new FeignException.NotFound("no esta", peticionFalsa(), null, null));

        assertThatThrownBy(() -> pasarela.consultarElegibilidad("CON-011", desde, hasta, "FURGON", "CLI-0019"))
                .isInstanceOf(ConductoresIntegrationException.class);
    }

    @Test
    void siConductoresNoContestaTambienEsUnFalloDeIntegracion() {
        when(cliente.consultarElegibilidad("CON-011", desde.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), hasta.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), "FURGON", "CLI-0019")).thenThrow(new RetryableException(
                -1, "Connection refused", Request.HttpMethod.GET, (Long) null, peticionFalsa()));

        assertThatThrownBy(() -> pasarela.consultarElegibilidad("CON-011", desde, hasta, "FURGON", "CLI-0019"))
                .isInstanceOf(ConductoresIntegrationException.class);
    }

    @Test
    void unCuerpoIncompletoEsUnFalloDeIntegracion() {
        when(cliente.consultarElegibilidad("CON-011", desde.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), hasta.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), "FURGON", "CLI-0019")).thenReturn(new ElegibilidadConductorRemota(
                "CON-011", true, null, null, null));

        assertThatThrownBy(() -> pasarela.consultarElegibilidad("CON-011", desde, hasta, "FURGON", "CLI-0019"))
                .isInstanceOf(ConductoresIntegrationException.class)
                .hasMessageContaining("incompleta");
    }
}
