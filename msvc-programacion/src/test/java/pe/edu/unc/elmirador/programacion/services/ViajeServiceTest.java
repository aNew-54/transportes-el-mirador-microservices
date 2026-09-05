package pe.edu.unc.elmirador.programacion.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import pe.edu.unc.elmirador.programacion.dto.request.CargaRequest;
import pe.edu.unc.elmirador.programacion.dto.request.PlanificarViajeRequest;
import pe.edu.unc.elmirador.programacion.dto.request.RutaRequest;
import pe.edu.unc.elmirador.programacion.dto.request.VentanaDeTiempoRequest;
import pe.edu.unc.elmirador.programacion.dto.response.ViajeResponse;
import pe.edu.unc.elmirador.programacion.exceptions.ConflictoDeRecursoException;
import pe.edu.unc.elmirador.programacion.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.programacion.models.entity.Viaje;
import pe.edu.unc.elmirador.programacion.models.vo.TipoDeCarga;
import pe.edu.unc.elmirador.programacion.repositories.AgendaDeConductorRepository;
import pe.edu.unc.elmirador.programacion.repositories.AgendaDeUnidadRepository;
import pe.edu.unc.elmirador.programacion.repositories.ViajeRepository;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import java.util.List;
import pe.edu.unc.elmirador.programacion.clients.EvaluacionDeUnidad;
import pe.edu.unc.elmirador.programacion.clients.OrdenConfirmada;
import pe.edu.unc.elmirador.programacion.dto.request.AsignarRecursosRequest;
import pe.edu.unc.elmirador.programacion.dto.request.CapacidadRequest;
import pe.edu.unc.elmirador.programacion.dto.request.ConsolidarOrdenRequest;
import pe.edu.unc.elmirador.programacion.exceptions.UnidadesIntegrationException;
import pe.edu.unc.elmirador.programacion.models.vo.ElegibilidadDeRecurso;
import pe.edu.unc.elmirador.programacion.clients.ComercialGateway;
import pe.edu.unc.elmirador.programacion.clients.UnidadesGateway;
import pe.edu.unc.elmirador.programacion.clients.ConductoresGateway;

class ViajeServiceTest {

    private ViajeRepository viajeRepository;
    private AgendaDeUnidadRepository agendaDeUnidadRepository;
    private AgendaDeConductorRepository agendaDeConductorRepository;
    private ComercialGateway comercialGateway;
    private UnidadesGateway unidadesGateway;
    private ConductoresGateway conductoresGateway;
    private ViajeService servicio;

    @BeforeEach
    void preparar() {
        viajeRepository = mock(ViajeRepository.class);
        agendaDeUnidadRepository = mock(AgendaDeUnidadRepository.class);
        agendaDeConductorRepository = mock(AgendaDeConductorRepository.class);
        comercialGateway = mock(ComercialGateway.class);
        unidadesGateway = mock(UnidadesGateway.class);
        conductoresGateway = mock(ConductoresGateway.class);
        servicio = new ViajeService(viajeRepository, agendaDeUnidadRepository, agendaDeConductorRepository, comercialGateway, unidadesGateway, conductoresGateway);

        when(viajeRepository.save(any(Viaje.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("planificar viaje con id existente lanza ConflictoDeRecursoException")
    void planificarConConflicto() {
        when(viajeRepository.existsById("v-1")).thenReturn(true);

        PlanificarViajeRequest req = new PlanificarViajeRequest(
                "v-1",
                new RutaRequest("Lima", "Arequipa", "Sur"),
                new VentanaDeTiempoRequest(OffsetDateTime.parse("2026-03-10T10:00:00Z"), OffsetDateTime.parse("2026-03-11T10:00:00Z")),
                new CargaRequest("ord-1", 1000, new BigDecimal("2.5"), TipoDeCarga.PALETIZADA, 1)
        );

        assertThatThrownBy(() -> servicio.planificar(req))
                .isInstanceOf(ConflictoDeRecursoException.class);
    }

    @Test
    @DisplayName("consultar viaje inexistente lanza RecursoNoEncontradoException")
    void consultarNoExiste() {
        when(viajeRepository.findById("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.consultar("no-existe"))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("no-existe");
    }

    @Test
    @DisplayName("despachar delega en el agregado y guarda")
    void despacharDelega() {
        Viaje viajeMock = mock(Viaje.class);
        when(viajeMock.id()).thenReturn("v-1");
        when(viajeRepository.findById("v-1")).thenReturn(Optional.of(viajeMock));

        servicio.despachar("v-1");

        verify(viajeMock).autorizarDespacho();
        verify(viajeRepository).save(viajeMock);
    }

    private static final VentanaDeTiempoRequest VENTANA_REQ = new VentanaDeTiempoRequest(
            OffsetDateTime.parse("2026-03-10T10:00:00Z"), OffsetDateTime.parse("2026-03-11T10:00:00Z"));

    private Viaje viajePlanificado() {
        return Viaje.planificar(
                "v-1",
                new pe.edu.unc.elmirador.programacion.models.vo.Ruta("Lima", "Arequipa", "Sur"),
                new pe.edu.unc.elmirador.programacion.models.vo.VentanaDeTiempo(
                        VENTANA_REQ.desde(), VENTANA_REQ.hasta()),
                new pe.edu.unc.elmirador.programacion.models.vo.Carga(
                        "ord-1", 1000, new BigDecimal("2.5"), TipoDeCarga.PALETIZADA, 1));
    }

    private OrdenConfirmada ordenConClausula(boolean permite) {
        return new OrdenConfirmada(
                "ord-2", "cli-1", 500, new BigDecimal("1.0"), TipoDeCarga.GENERAL,
                new pe.edu.unc.elmirador.programacion.models.vo.Ruta("Lima", "Arequipa", "Sur"),
                new pe.edu.unc.elmirador.programacion.models.vo.VentanaDeTiempo(
                        VENTANA_REQ.desde(), VENTANA_REQ.hasta()),
                new pe.edu.unc.elmirador.programacion.models.vo.ClausulaDeConsolidacion(permite, List.of()),
                "FURGON");
    }

    /**
     * VIA-04. La clausula del contrato marco la trae el contrato 1, no el cuerpo de la peticion.
     * Antes de cablear esto, quien pedia consolidar enviaba tambien la clausula que decide si se puede:
     * mandarla permisiva bastaba para que la invariante no pudiera fallar nunca.
     */
    @Test
    @DisplayName("consolidar pide la orden a Comercial y respeta SU clausula")
    void consolidarUsaLaClausulaDeComercial() {
        when(viajeRepository.findById("v-1")).thenReturn(Optional.of(viajePlanificado()));
        when(comercialGateway.obtenerOrden("ord-2")).thenReturn(ordenConClausula(false));

        ConsolidarOrdenRequest req = new ConsolidarOrdenRequest(
                "ord-2", 2, new CapacidadRequest(20000, new BigDecimal("60.0")));

        assertThatThrownBy(() -> servicio.consolidarOrden("v-1", req))
                .isInstanceOf(pe.edu.unc.elmirador.programacion.exceptions.ConsolidacionProhibidaException.class);
        verify(comercialGateway).obtenerOrden("ord-2");
    }

    /** Contratos 2 y 3: la elegibilidad se pregunta, no se recibe. */
    @Test
    @DisplayName("asignar recursos consulta a Unidades y a Conductores")
    void asignarConsultaLosDosProveedores() {
        Viaje viaje = viajePlanificado();
        when(viajeRepository.findById("v-1")).thenReturn(Optional.of(viaje));
        when(comercialGateway.obtenerOrden("ord-1")).thenReturn(ordenConClausula(true));
        when(unidadesGateway.consultarElegibilidad(eq("u-1"), any(), any(), anyInt(), any(), any()))
                .thenReturn(new EvaluacionDeUnidad(
                        ElegibilidadDeRecurso.recursoElegible(),
                        new pe.edu.unc.elmirador.programacion.models.vo.Capacidad(20000, new BigDecimal("60.0")),
                        "FURGON"));
        when(conductoresGateway.consultarElegibilidad(eq("c-1"), any(), any(), any(), any()))
                .thenReturn(ElegibilidadDeRecurso.recursoElegible());
        when(agendaDeUnidadRepository.findById("u-1")).thenReturn(Optional.empty());
        when(agendaDeConductorRepository.findById("c-1")).thenReturn(Optional.empty());

        servicio.asignarRecursos("v-1", new AsignarRecursosRequest("u-1", List.of("c-1"), false));

        verify(unidadesGateway).consultarElegibilidad(eq("u-1"), any(), any(), anyInt(), any(), any());
        verify(conductoresGateway).consultarElegibilidad(eq("c-1"), any(), any(), eq("FURGON"), eq("cli-1"));
    }

    /**
     * Si Unidades no responde, no se asigna. No se supone elegible lo que no se pudo comprobar:
     * es la misma decision que el contrato 11 escribe explicita para Comercial y Cobranza.
     */
    @Test
    @DisplayName("con Unidades caida no se reserva nada")
    void asignarConUnidadesCaidaNoReservaNada() {
        when(viajeRepository.findById("v-1")).thenReturn(Optional.of(viajePlanificado()));
        when(comercialGateway.obtenerOrden("ord-1")).thenReturn(ordenConClausula(true));
        when(unidadesGateway.consultarElegibilidad(eq("u-1"), any(), any(), anyInt(), any(), any()))
                .thenThrow(new UnidadesIntegrationException("Unidades no respondio"));

        assertThatThrownBy(() -> servicio.asignarRecursos(
                "v-1", new AsignarRecursosRequest("u-1", List.of("c-1"), false)))
                .isInstanceOf(UnidadesIntegrationException.class);

        verify(agendaDeUnidadRepository, never()).save(any());
        verify(agendaDeConductorRepository, never()).save(any());
        verify(conductoresGateway, never()).consultarElegibilidad(any(), any(), any(), any(), any());
    }
}
