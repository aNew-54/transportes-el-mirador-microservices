package pe.edu.unc.elmirador.unidades.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.edu.unc.elmirador.unidades.dto.internal.request.ReportarFallaRequest;
import pe.edu.unc.elmirador.unidades.dto.internal.request.ReportarKilometrajeRequest;
import pe.edu.unc.elmirador.unidades.dto.internal.response.ElegibilidadUnidadResponse;
import pe.edu.unc.elmirador.unidades.dto.internal.response.FallaRegistradaResponse;
import pe.edu.unc.elmirador.unidades.dto.internal.response.KilometrajeRegistradoResponse;
import pe.edu.unc.elmirador.unidades.dto.response.ResultadoIdempotente;
import pe.edu.unc.elmirador.unidades.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.unidades.models.entity.PeticionIdempotente;
import pe.edu.unc.elmirador.unidades.models.entity.Unidad;
import pe.edu.unc.elmirador.unidades.models.vo.Capacidad;
import pe.edu.unc.elmirador.unidades.models.vo.EstadoOperativo;
import pe.edu.unc.elmirador.unidades.models.vo.IntervaloDeMantenimiento;
import pe.edu.unc.elmirador.unidades.models.vo.Kilometraje;
import pe.edu.unc.elmirador.unidades.models.vo.Placa;
import pe.edu.unc.elmirador.unidades.models.vo.ProgramaDeMantenimiento;
import pe.edu.unc.elmirador.unidades.models.vo.SituacionOperativa;
import pe.edu.unc.elmirador.unidades.models.vo.TipoDeCarga;
import pe.edu.unc.elmirador.unidades.models.vo.TipoDeUnidad;
import pe.edu.unc.elmirador.unidades.repositories.PeticionIdempotenteRepository;
import pe.edu.unc.elmirador.unidades.repositories.UnidadRepository;

@ExtendWith(MockitoExtension.class)
class UnidadInternalServiceTest {

    @Mock
    private UnidadRepository unidadRepository;

    @Mock
    private PeticionIdempotenteRepository peticionIdempotenteRepository;

    private UnidadInternalService servicio;
    private Clock relojFijo;
    private Unidad unidad;

    @BeforeEach
    void setUp() {
        relojFijo = Clock.fixed(Instant.parse("2026-09-10T16:00:00Z"), ZoneId.of("UTC"));
        servicio = new UnidadInternalService(unidadRepository, peticionIdempotenteRepository, relojFijo);
        
        unidad = new Unidad(
                "UNI-004",
                new Placa("ABC-123"),
                TipoDeUnidad.FURGON,
                new Capacidad(10000, new BigDecimal("32.00")),
                new Kilometraje(180000),
                EstadoOperativo.operativa(),
                new ProgramaDeMantenimiento(
                        new Kilometraje(180000),
                        new Kilometraje(190000),
                        IntervaloDeMantenimiento.ACEITE_Y_FILTROS),
                List.of()
        );
    }

    @Test
    void elegibilidad_encuentraUnidad_retornaMotivos() {
        when(unidadRepository.findById("UNI-004")).thenReturn(Optional.of(unidad));
        unidad.marcarInoperativa("Freno fallido");

        ElegibilidadUnidadResponse response = servicio.elegibilidad(
                "UNI-004",
                OffsetDateTime.now(relojFijo),
                OffsetDateTime.now(relojFijo),
                5000,
                new BigDecimal("10.0"),
                TipoDeCarga.GENERAL
        );

        assertThat(response.unidadId()).isEqualTo("UNI-004");
        assertThat(response.elegible()).isFalse();
        assertThat(response.motivos()).contains("INOPERATIVA");
        assertThat(response.capacidad().pesoMaximoKg()).isEqualTo(10000);
        assertThat(response.capacidad().volumenMaximoM3()).isEqualTo(new BigDecimal("32.00"));
        assertThat(response.tipoUnidad()).isEqualTo("FURGON");
        assertThat(response.estadoOperativo()).isEqualTo("INOPERATIVA");
    }

    @Test
    void reportarKilometraje_primeraVez_registraYGuardaClave() {
        String clave = "VIA-01:km-final";
        ReportarKilometrajeRequest req = new ReportarKilometrajeRequest("VIA-01", 185000, OffsetDateTime.now(relojFijo));
        when(peticionIdempotenteRepository.findById(clave)).thenReturn(Optional.empty());
        when(unidadRepository.findById("UNI-004")).thenReturn(Optional.of(unidad));

        ResultadoIdempotente<KilometrajeRegistradoResponse> res = servicio.reportarKilometraje("UNI-004", clave, req);

        assertThat(res.repetida()).isFalse();
        assertThat(res.cuerpo().unidadId()).isEqualTo("UNI-004");
        
        verify(unidadRepository, times(1)).save(unidad);
        verify(peticionIdempotenteRepository, times(1)).save(any(PeticionIdempotente.class));
        assertThat(unidad.getKilometraje().valor()).isEqualTo(185000);
    }

    @Test
    void reportarKilometraje_idempotente_reintentoDevuelveOriginalSinTocarAgregado() {
        String clave = "VIA-01:km-final";
        ReportarKilometrajeRequest req = new ReportarKilometrajeRequest("VIA-01", 185000, OffsetDateTime.now(relojFijo));
        PeticionIdempotente previa = new PeticionIdempotente(clave, "UNI-004", OffsetDateTime.now(relojFijo));
        
        when(peticionIdempotenteRepository.findById(clave)).thenReturn(Optional.of(previa));

        ResultadoIdempotente<KilometrajeRegistradoResponse> res = servicio.reportarKilometraje("UNI-004", clave, req);

        assertThat(res.repetida()).isTrue();
        assertThat(res.cuerpo().unidadId()).isEqualTo("UNI-004");
        
        verify(unidadRepository, never()).findById(any());
        verify(unidadRepository, never()).save(any());
        verify(peticionIdempotenteRepository, never()).save(any());
    }

    @Test
    void reportarFalla_dejaInoperativa_marcaUnidadInoperativa() {
        String clave = "VIA-01:falla";
        ReportarFallaRequest req = new ReportarFallaRequest("VIA-01", "MECANICA", "Fallo motor", OffsetDateTime.now(relojFijo), true);
        when(peticionIdempotenteRepository.findById(clave)).thenReturn(Optional.empty());
        when(unidadRepository.findById("UNI-004")).thenReturn(Optional.of(unidad));

        ResultadoIdempotente<FallaRegistradaResponse> res = servicio.reportarFalla("UNI-004", clave, req);

        assertThat(res.repetida()).isFalse();
        assertThat(unidad.getEstadoOperativo().situacion()).isEqualTo(SituacionOperativa.INOPERATIVA);
        assertThat(unidad.getEstadoOperativo().motivo()).isEqualTo("Fallo motor");
        
        verify(unidadRepository, times(1)).save(unidad);
        verify(peticionIdempotenteRepository, times(1)).save(any(PeticionIdempotente.class));
    }

    @Test
    void reportarFalla_idempotente_reintentoNoHaceNada() {
        String clave = "VIA-01:falla";
        ReportarFallaRequest req = new ReportarFallaRequest("VIA-01", "MECANICA", "Fallo motor", OffsetDateTime.now(), true);
        PeticionIdempotente previa = new PeticionIdempotente(clave, "UNI-004", OffsetDateTime.now());
        
        when(peticionIdempotenteRepository.findById(clave)).thenReturn(Optional.of(previa));

        ResultadoIdempotente<FallaRegistradaResponse> res = servicio.reportarFalla("UNI-004", clave, req);

        assertThat(res.repetida()).isTrue();
        
        verify(unidadRepository, never()).save(any());
        verify(peticionIdempotenteRepository, never()).save(any());
    }
}
