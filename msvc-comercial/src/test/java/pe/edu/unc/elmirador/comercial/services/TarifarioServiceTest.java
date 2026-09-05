package pe.edu.unc.elmirador.comercial.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.edu.unc.elmirador.comercial.dto.request.RegistrarTarifarioRequest;
import pe.edu.unc.elmirador.comercial.dto.response.TarifarioResponse;
import pe.edu.unc.elmirador.comercial.exceptions.TarifarioVigenteDuplicadoException;
import pe.edu.unc.elmirador.comercial.models.entity.Tarifario;
import pe.edu.unc.elmirador.comercial.models.vo.PeriodoDeVigencia;
import pe.edu.unc.elmirador.comercial.repositories.TarifarioRepository;

class TarifarioServiceTest {

    private TarifarioRepository repositorio;
    private Clock reloj;
    private TarifarioService servicio;

    @BeforeEach
    void setUp() {
        repositorio = mock(TarifarioRepository.class);
        reloj = Clock.fixed(Instant.parse("2026-09-04T10:00:00Z"), ZoneId.of("America/Lima"));
        servicio = new TarifarioService(repositorio, reloj);
    }

    @Test
    void publicar_sinSolapamiento_guardaYDevuelveRespuesta() {
        RegistrarTarifarioRequest peticion = new RegistrarTarifarioRequest(
                LocalDate.parse("2026-09-05"), LocalDate.parse("2026-12-31"), List.of(), List.of());

        Tarifario vigente = new Tarifario("tar-prev", 
                new PeriodoDeVigencia(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-09-04")), 
                List.of(), List.of());

        when(repositorio.findAll()).thenReturn(List.of(vigente));
        when(repositorio.save(any(Tarifario.class))).thenAnswer(i -> {
            Tarifario t = i.getArgument(0);
            return new Tarifario("tar-nuevo", t.vigencia(), t.precios(), t.recargosEstandar());
        });

        TarifarioResponse respuesta = servicio.publicar(peticion);

        assertEquals("tar-nuevo", respuesta.id());
        verify(repositorio).save(any(Tarifario.class));
    }

    @Test
    void publicar_conSolapamiento_lanzaTarifarioVigenteDuplicado() {
        RegistrarTarifarioRequest peticion = new RegistrarTarifarioRequest(
                LocalDate.parse("2026-09-01"), LocalDate.parse("2026-12-31"), List.of(), List.of());

        Tarifario vigente = new Tarifario("tar-prev", 
                new PeriodoDeVigencia(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-09-04")), 
                List.of(), List.of());

        when(repositorio.findAll()).thenReturn(List.of(vigente));

        assertThrows(TarifarioVigenteDuplicadoException.class, () -> servicio.publicar(peticion));
    }
}
