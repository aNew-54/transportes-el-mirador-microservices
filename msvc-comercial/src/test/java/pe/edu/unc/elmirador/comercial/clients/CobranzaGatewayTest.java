package pe.edu.unc.elmirador.comercial.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.RetryableException;
import pe.edu.unc.elmirador.comercial.clients.dto.EstadoCrediticioRemoto;
import pe.edu.unc.elmirador.comercial.clients.dto.ImporteRemoto;
import pe.edu.unc.elmirador.comercial.exceptions.CobranzaIntegrationException;
import pe.edu.unc.elmirador.comercial.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.comercial.models.vo.EstadoCrediticio;
import pe.edu.unc.elmirador.comercial.models.vo.SituacionCrediticia;

/**
 * Contrato 11, lado consumidor. Demuestra la regla 5 de contracts.md: todo fallo remoto se traduce a
 * una excepcion de integracion propia y ninguno se convierte en un 404 ni en un valor por defecto.
 */
class CobranzaGatewayTest {

    private CobranzaClient cliente;
    private CobranzaGateway pasarela;

    @BeforeEach
    void setUp() {
        cliente = mock(CobranzaClient.class);
        pasarela = new CobranzaGateway(cliente);
    }

    private static Request peticionFalsa() {
        return Request.create(Request.HttpMethod.GET, "/internal/v1/clientes/cli-1/estado-crediticio",
                java.util.Map.of(), null, new RequestTemplate());
    }

    @Test
    void traduceLaRespuestaDelContratoAlObjetoDeValorPropio() {
        when(cliente.estadoCrediticio("cli-1")).thenReturn(new EstadoCrediticioRemoto(
                "cli-1", "SUSPENDIDO", LocalDate.parse("2026-08-28"), 43, 2,
                List.of(new ImporteRemoto("5420.30", "PEN"), new ImporteRemoto("800.00", "USD"))));

        EstadoCrediticio estado = pasarela.estadoCrediticioDe("cli-1");

        assertThat(estado.situacion()).isEqualTo(SituacionCrediticia.SUSPENDIDO);
        assertThat(estado.fechaDeCambio()).isEqualTo(LocalDate.parse("2026-08-28"));
        assertThat(estado.permiteCredito()).isFalse();
    }

    @Test
    void unErrorDelServidorRemotoEsUnFalloDeIntegracion() {
        when(cliente.estadoCrediticio("cli-1"))
                .thenThrow(new FeignException.InternalServerError("boom", peticionFalsa(), null, null));

        assertThatThrownBy(() -> pasarela.estadoCrediticioDe("cli-1"))
                .isInstanceOf(CobranzaIntegrationException.class)
                .hasMessageContaining("cli-1");
    }

    /**
     * Regla 5, la mitad que es facil de incumplir. Un 404 de Cobranza invita a devolver un vacio o a
     * relanzar el 404 propio, y las dos cosas mienten: no significa que el cliente no exista, significa
     * que los dos contextos discrepan sobre que clientes hay.
     */
    @Test
    void unCuatrocientosCuatroRemotoNoSeConvierteEnUnCuatrocientosCuatroPropio() {
        when(cliente.estadoCrediticio("cli-1"))
                .thenThrow(new FeignException.NotFound("no esta", peticionFalsa(), null, null));

        assertThatThrownBy(() -> pasarela.estadoCrediticioDe("cli-1"))
                .isInstanceOf(CobranzaIntegrationException.class)
                .isNotInstanceOf(RecursoNoEncontradoException.class);
    }

    /**
     * El caso que este slice existe para cubrir. RetryableException NO hereda de FeignException: es la
     * que sale cuando el socket no abre o vence el read-timeout, y con un catch de FeignException a
     * secas se escaparia sin traducir justo cuando el proveedor esta caido.
     */
    @Test
    void siCobranzaNoContestaTambienEsUnFalloDeIntegracion() {
        when(cliente.estadoCrediticio("cli-1")).thenThrow(new RetryableException(
                -1, "Connection refused", Request.HttpMethod.GET, (Long) null, peticionFalsa()));

        assertThatThrownBy(() -> pasarela.estadoCrediticioDe("cli-1"))
                .isInstanceOf(CobranzaIntegrationException.class)
                .hasMessageContaining("no respondio");
    }

    @Test
    void unaSituacionQueComercialNoConoceEsUnFalloDeIntegracion() {
        when(cliente.estadoCrediticio("cli-1")).thenReturn(new EstadoCrediticioRemoto(
                "cli-1", "EN_OBSERVACION", LocalDate.parse("2026-08-28"), 0, 0, List.of()));

        assertThatThrownBy(() -> pasarela.estadoCrediticioDe("cli-1"))
                .isInstanceOf(CobranzaIntegrationException.class)
                .hasMessageContaining("EN_OBSERVACION");
    }

    @Test
    void unCuerpoIncompletoEsUnFalloDeIntegracion() {
        when(cliente.estadoCrediticio("cli-1")).thenReturn(new EstadoCrediticioRemoto(
                "cli-1", null, null, 0, 0, List.of()));

        assertThatThrownBy(() -> pasarela.estadoCrediticioDe("cli-1"))
                .isInstanceOf(CobranzaIntegrationException.class)
                .hasMessageContaining("incompleto");
    }

    /** Ninguna respuesta rara produce un VIGENTE. El contrato 11 lo prohibe expresamente. */
    @Test
    void nuncaDevuelveVigentePorDefecto() {
        when(cliente.estadoCrediticio("cli-1"))
                .thenThrow(new FeignException.ServiceUnavailable("caido", peticionFalsa(), null, null));

        assertThatThrownBy(() -> pasarela.estadoCrediticioDe("cli-1"))
                .isInstanceOf(CobranzaIntegrationException.class);
    }
}
