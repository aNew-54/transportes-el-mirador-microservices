package pe.edu.unc.elmirador.unidades.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import pe.edu.unc.elmirador.unidades.dto.request.RegistrarUnidadRequest;
import pe.edu.unc.elmirador.unidades.dto.response.UnidadResponse;
import pe.edu.unc.elmirador.unidades.exceptions.ConflictoDeRecursoException;
import pe.edu.unc.elmirador.unidades.models.entity.Unidad;
import pe.edu.unc.elmirador.unidades.models.vo.Capacidad;
import pe.edu.unc.elmirador.unidades.models.vo.EstadoOperativo;
import pe.edu.unc.elmirador.unidades.models.vo.IntervaloDeMantenimiento;
import pe.edu.unc.elmirador.unidades.models.vo.Kilometraje;
import pe.edu.unc.elmirador.unidades.models.vo.Placa;
import pe.edu.unc.elmirador.unidades.models.vo.ProgramaDeMantenimiento;
import pe.edu.unc.elmirador.unidades.models.vo.TipoDeUnidad;
import pe.edu.unc.elmirador.unidades.repositories.UnidadRepository;

class UnidadServiceTest {

    private UnidadRepository repositorio;
    private Clock reloj;
    private UnidadService servicio;

    @BeforeEach
    void setUp() {
        repositorio = mock(UnidadRepository.class);
        reloj = Clock.fixed(Instant.parse("2026-03-10T10:00:00Z"), ZoneId.of("America/Lima"));
        servicio = new UnidadService(repositorio, reloj);
    }

    @Test
    @DisplayName("registrar guarda la unidad y devuelve response")
    void registrar() {
        when(repositorio.findByPlacaValor("ABC-123")).thenReturn(Optional.empty());
        when(repositorio.save(any(Unidad.class))).thenAnswer(i -> i.getArgument(0));

        RegistrarUnidadRequest request = new RegistrarUnidadRequest(
                "ABC-123", TipoDeUnidad.FURGON, 10000, new BigDecimal("30.00"), 50000, IntervaloDeMantenimiento.ACEITE_Y_FILTROS
        );

        UnidadResponse response = servicio.registrar(request);

        assertThat(response.placa()).isEqualTo("ABC-123");
        verify(repositorio).save(any(Unidad.class));
    }

    @Test
    @DisplayName("registrar con placa repetida lanza ConflictoDeRecursoException")
    void registrarRepetida() {
        when(repositorio.findByPlacaValor("ABC-123")).thenReturn(Optional.of(mock(Unidad.class)));

        RegistrarUnidadRequest request = new RegistrarUnidadRequest(
                "ABC-123", TipoDeUnidad.FURGON, 10000, new BigDecimal("30.00"), 50000, IntervaloDeMantenimiento.ACEITE_Y_FILTROS
        );

        Throwable error = catchThrowable(() -> servicio.registrar(request));

        assertThat(error)
                .isInstanceOf(ConflictoDeRecursoException.class)
                .hasMessageContaining("ABC-123");
    }
}
