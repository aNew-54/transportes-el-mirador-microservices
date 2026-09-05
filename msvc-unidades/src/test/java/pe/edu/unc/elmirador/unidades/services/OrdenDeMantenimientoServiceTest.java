package pe.edu.unc.elmirador.unidades.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import pe.edu.unc.elmirador.unidades.dto.request.AbrirOrdenRequest;
import pe.edu.unc.elmirador.unidades.dto.response.OrdenDeMantenimientoResponse;
import pe.edu.unc.elmirador.unidades.exceptions.KilometrajeDeAtencionInvalidoException;
import pe.edu.unc.elmirador.unidades.models.entity.OrdenDeMantenimiento;
import pe.edu.unc.elmirador.unidades.models.entity.Unidad;
import pe.edu.unc.elmirador.unidades.models.vo.Kilometraje;
import pe.edu.unc.elmirador.unidades.models.vo.ProgramaDeMantenimiento;
import pe.edu.unc.elmirador.unidades.models.vo.TipoDeMantenimiento;
import pe.edu.unc.elmirador.unidades.repositories.OrdenDeMantenimientoRepository;
import pe.edu.unc.elmirador.unidades.repositories.UnidadRepository;

class OrdenDeMantenimientoServiceTest {

    private OrdenDeMantenimientoRepository ordenRepository;
    private UnidadRepository unidadRepository;
    private Clock reloj;
    private OrdenDeMantenimientoService servicio;

    @BeforeEach
    void setUp() {
        ordenRepository = mock(OrdenDeMantenimientoRepository.class);
        unidadRepository = mock(UnidadRepository.class);
        reloj = Clock.fixed(Instant.parse("2026-03-10T10:00:00Z"), ZoneId.of("America/Lima"));
        servicio = new OrdenDeMantenimientoService(ordenRepository, unidadRepository, reloj);
    }

    @Test
    @DisplayName("abrir guarda la orden y devuelve response")
    void abrir() {
        Unidad unidad = mock(Unidad.class);
        when(unidad.getId()).thenReturn("u-1");
        ProgramaDeMantenimiento programa = mock(ProgramaDeMantenimiento.class);
        when(programa.kmUltimoServicio()).thenReturn(new Kilometraje(10000));
        when(unidad.getProgramaDeMantenimiento()).thenReturn(programa);
        when(unidadRepository.findById("u-1")).thenReturn(Optional.of(unidad));
        
        when(ordenRepository.save(any(OrdenDeMantenimiento.class))).thenAnswer(i -> i.getArgument(0));

        AbrirOrdenRequest request = new AbrirOrdenRequest("u-1", TipoDeMantenimiento.PREVENTIVO, 15000, "PEN");

        OrdenDeMantenimientoResponse response = servicio.abrir(request);

        assertThat(response.unidadId()).isEqualTo("u-1");
        verify(ordenRepository).save(any(OrdenDeMantenimiento.class));
    }

    @Test
    @DisplayName("abrir con km menor al ultimo servicio lanza excepcion del dominio sin transformar")
    void abrirConKmMenor() {
        Unidad unidad = mock(Unidad.class);
        when(unidad.getId()).thenReturn("u-1");
        ProgramaDeMantenimiento programa = mock(ProgramaDeMantenimiento.class);
        when(programa.kmUltimoServicio()).thenReturn(new Kilometraje(20000));
        when(unidad.getProgramaDeMantenimiento()).thenReturn(programa);
        when(unidadRepository.findById("u-1")).thenReturn(Optional.of(unidad));

        AbrirOrdenRequest request = new AbrirOrdenRequest("u-1", TipoDeMantenimiento.PREVENTIVO, 15000, "PEN");

        Throwable error = catchThrowable(() -> servicio.abrir(request));

        assertThat(error).isInstanceOf(KilometrajeDeAtencionInvalidoException.class);
    }
}
