package pe.edu.unc.elmirador.programacion.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.unc.elmirador.programacion.dto.internal.response.HojaDeRutaContratoResponse;
import pe.edu.unc.elmirador.programacion.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.programacion.exceptions.TransicionDeViajeInvalidaException;
import pe.edu.unc.elmirador.programacion.models.entity.Viaje;
import pe.edu.unc.elmirador.programacion.models.vo.AsignacionDeRecursos;
import pe.edu.unc.elmirador.programacion.models.vo.Carga;
import pe.edu.unc.elmirador.programacion.models.vo.HojaDeRuta;
import pe.edu.unc.elmirador.programacion.models.vo.Parada;
import pe.edu.unc.elmirador.programacion.models.vo.Ruta;
import pe.edu.unc.elmirador.programacion.models.vo.VentanaDeTiempo;
import pe.edu.unc.elmirador.programacion.repositories.ViajeRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import pe.edu.unc.elmirador.programacion.models.vo.TipoDeCarga;
import pe.edu.unc.elmirador.programacion.models.vo.Ubicacion;

@ExtendWith(MockitoExtension.class)
class ViajeInternalServiceTest {

    private static final OffsetDateTime BASE =
            OffsetDateTime.of(2026, 9, 10, 6, 0, 0, 0, ZoneOffset.of("-05:00"));

    @Mock
    private ViajeRepository repositorio;

    @InjectMocks
    private ViajeInternalService servicio;

    private Viaje viajeProgramado;
    private Viaje viajePlanificado;

    @BeforeEach
    void setUp() {
        viajePlanificado = Viaje.planificar(
                "V-001",
                new Ruta("Lima", "Trujillo", "COSTA_NORTE"),
                new VentanaDeTiempo(BASE, BASE.plusHours(24)),
                new Carga("ORD-01", 1000, new BigDecimal("15.0"), TipoDeCarga.GENERAL, 1)
        );

        viajeProgramado = Viaje.planificar(
                "V-002",
                new Ruta("Lima", "Arequipa", "SUR"),
                new VentanaDeTiempo(BASE, BASE.plusHours(24)),
                new Carga("ORD-02", 1000, new BigDecimal("15.0"), TipoDeCarga.GENERAL, 1)
        );
        viajeProgramado.asignarRecursos(AsignacionDeRecursos.conRelevo("U-01", "C-01", "C-02"));
        Parada parada1 = new Parada(1, Parada.CARGA, "ORD-02",
                new Ubicacion("Jr. Ayacucho 450", "Cajamarca", "Almacen 2", "+51 976 000 111"),
                BASE.plusMinutes(30));
        Parada parada2 = new Parada(2, Parada.DESCARGA, "ORD-02",
                new Ubicacion("Av. Espana 1200", "Trujillo", "Puerta 3", "+51 944 222 333"),
                BASE.plusHours(8));
        viajeProgramado.confirmarProgramacion(new HojaDeRuta(
                List.of(parada1, parada2), "Coordinar con almacen del cliente antes de las 07:00."));
    }

    @Test
    void obtenerHojaDeRutaEjecutable_LanzaRecursoNoEncontrado_CuandoViajeNoExiste() {
        when(repositorio.findById("V-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtenerHojaDeRutaEjecutable("V-999"))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("viaje")
                .hasMessageContaining("V-999");
    }

    @Test
    void obtenerHojaDeRutaEjecutable_LanzaTransicionInvalida_CuandoEstaPlanificado() {
        when(repositorio.findById("V-001")).thenReturn(Optional.of(viajePlanificado));

        assertThatThrownBy(() -> servicio.obtenerHojaDeRutaEjecutable("V-001"))
                .isInstanceOf(TransicionDeViajeInvalidaException.class)
                .hasMessageContaining("V-001 esta en PLANIFICADO");
    }

    @Test
    void obtenerHojaDeRutaEjecutable_DevuelveResponse_CuandoEstaProgramado() {
        when(repositorio.findById("V-002")).thenReturn(Optional.of(viajeProgramado));

        HojaDeRutaContratoResponse response = servicio.obtenerHojaDeRutaEjecutable("V-002");

        assertThat(response.viajeId()).isEqualTo("V-002");
        assertThat(response.estado()).isEqualTo("PROGRAMADO");
        assertThat(response.unidadId()).isEqualTo("U-01");
        assertThat(response.conductorIds()).containsExactly("C-01", "C-02");
        assertThat(response.paradas()).hasSize(2);
        assertThat(response.paradas().get(0).secuencia()).isEqualTo(1);
        assertThat(response.paradas().get(0).tipo()).isEqualTo("CARGA");
        assertThat(response.paradas().get(1).secuencia()).isEqualTo(2);
        assertThat(response.paradas().get(1).tipo()).isEqualTo("DESCARGA");

        // Los cuatro campos que el contrato 4 pide y que hasta S4 el dominio no guardaba.
        assertThat(response.observaciones()).isEqualTo("Coordinar con almacen del cliente antes de las 07:00.");
        assertThat(response.paradas().get(0).ubicacion().direccion()).isEqualTo("Jr. Ayacucho 450");
        assertThat(response.paradas().get(0).ubicacion().distrito()).isEqualTo("Cajamarca");
        assertThat(response.paradas().get(0).ubicacion().referencia()).isEqualTo("Almacen 2");
        assertThat(response.paradas().get(0).ubicacion().contacto()).isEqualTo("+51 976 000 111");
    }
}
