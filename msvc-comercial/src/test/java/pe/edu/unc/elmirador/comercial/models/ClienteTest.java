package pe.edu.unc.elmirador.comercial.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.comercial.models.entity.Cliente;
import pe.edu.unc.elmirador.comercial.models.vo.CondicionDePago;
import pe.edu.unc.elmirador.comercial.models.vo.EstadoCrediticio;
import pe.edu.unc.elmirador.comercial.models.vo.RazonSocial;
import pe.edu.unc.elmirador.comercial.models.vo.Ruc;
import pe.edu.unc.elmirador.comercial.models.vo.SituacionCrediticia;

class ClienteTest {

    private final String clienteId = "CLI-0007";
    private final Ruc ruc = new Ruc("20481234567");
    private final RazonSocial razonSocial = new RazonSocial("Distribuidora Norte S.A.C.");
    private final CondicionDePago contado = CondicionDePago.contado();
    private final LocalDate hoy = LocalDate.of(2026, 9, 10);

    @Test
    @DisplayName("CLI-01: Cliente con credito SUSPENDIDO no puede contratar a credito (puedeContratarACredito es false)")
    void cli01_clienteSuspendidoNoPuedeContratarACredito() {
        EstadoCrediticio suspendido = EstadoCrediticio.suspendido(hoy.minusDays(5));
        Cliente cliente = new Cliente(clienteId, ruc, razonSocial, contado, suspendido);

        assertThat(cliente.puedeContratarACredito()).isFalse();
        assertThat(cliente.estadoCrediticio().situacion()).isEqualTo(SituacionCrediticia.SUSPENDIDO);
    }

    @Test
    @DisplayName("CLI-01: Cliente con credito SUSPENDIDO si puede contratar al contado (puedeContratarAlContado es true)")
    void cli01_clienteSuspendidoSiPuedeContratarAlContado() {
        EstadoCrediticio suspendido = EstadoCrediticio.suspendido(hoy.minusDays(5));
        Cliente cliente = new Cliente(clienteId, ruc, razonSocial, contado, suspendido);

        assertThat(cliente.puedeContratarAlContado()).isTrue();
    }

    @Test
    @DisplayName("CLI-01: Cliente con credito VIGENTE puede contratar tanto a credito como al contado")
    void cli01_clienteVigentePuedeContratarACreditoYAlContado() {
        EstadoCrediticio vigente = EstadoCrediticio.vigente(hoy.minusDays(10));
        Cliente cliente = new Cliente(clienteId, ruc, razonSocial, contado, vigente);

        assertThat(cliente.puedeContratarACredito()).isTrue();
        assertThat(cliente.puedeContratarAlContado()).isTrue();
    }

    @Test
    @DisplayName("Cliente refresca su copia local de EstadoCrediticio con una lectura contemporanea o posterior")
    void debeRefrescarEstadoCrediticioCorrectamente() {
        EstadoCrediticio inicial = EstadoCrediticio.vigente(hoy.minusDays(10));
        Cliente cliente = new Cliente(clienteId, ruc, razonSocial, contado, inicial);

        EstadoCrediticio actualizado = EstadoCrediticio.suspendido(hoy);
        cliente.refrescarEstadoCrediticio(actualizado);

        assertThat(cliente.estadoCrediticio()).isEqualTo(actualizado);
        assertThat(cliente.puedeContratarACredito()).isFalse();
    }

    @Test
    @DisplayName("Cliente rechaza refrescar estado crediticio si la fecha leida es anterior a la vigente")
    void debeRechazarRefrescoDeEstadoConFechaAnterior() {
        EstadoCrediticio vigente = EstadoCrediticio.vigente(hoy);
        Cliente cliente = new Cliente(clienteId, ruc, razonSocial, contado, vigente);

        EstadoCrediticio lecturaAntigua = EstadoCrediticio.suspendido(hoy.minusDays(1));

        assertThatThrownBy(() -> cliente.refrescarEstadoCrediticio(lecturaAntigua))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("anterior a la vigente");
    }

    @Test
    @DisplayName("Constructor de Cliente valida obligatoriedad de todos sus atributos")
    void debeValidarParametrosObligatoriosEnCliente() {
        EstadoCrediticio vigente = EstadoCrediticio.vigente(hoy);

        assertThatThrownBy(() -> new Cliente(null, ruc, razonSocial, contado, vigente))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Cliente(clienteId, null, razonSocial, contado, vigente))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Cliente(clienteId, ruc, null, contado, vigente))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Cliente(clienteId, ruc, razonSocial, null, vigente))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Cliente(clienteId, ruc, razonSocial, contado, null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
