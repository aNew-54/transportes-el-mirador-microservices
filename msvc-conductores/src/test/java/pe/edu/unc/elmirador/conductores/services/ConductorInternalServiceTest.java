package pe.edu.unc.elmirador.conductores.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import pe.edu.unc.elmirador.conductores.dto.internal.request.ReportarHorasRequest;
import pe.edu.unc.elmirador.conductores.dto.internal.request.ReportarIncidenciaRequest;
import pe.edu.unc.elmirador.conductores.exceptions.HorasExcedidasException;
import pe.edu.unc.elmirador.conductores.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.conductores.models.entity.Conductor;
import pe.edu.unc.elmirador.conductores.models.entity.Induccion;
import pe.edu.unc.elmirador.conductores.models.entity.PeticionIdempotente;
import pe.edu.unc.elmirador.conductores.models.vo.CategoriaDeLicencia;
import pe.edu.unc.elmirador.conductores.models.vo.EstadoDeHabilitacion;
import pe.edu.unc.elmirador.conductores.models.vo.HorasDeConduccion;
import pe.edu.unc.elmirador.conductores.models.vo.NumeroDeLicencia;
import pe.edu.unc.elmirador.conductores.models.vo.PeriodoDeVigencia;
import pe.edu.unc.elmirador.conductores.models.vo.TipoDeUnidad;
import pe.edu.unc.elmirador.conductores.repositories.ConductorRepository;
import pe.edu.unc.elmirador.conductores.repositories.PeticionIdempotenteRepository;

/** Lado proveedor de los contratos 3 y 6. */
class ConductorInternalServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 9, 10);
    private static final OffsetDateTime DESDE = OffsetDateTime.of(2026, 9, 10, 6, 0, 0, 0, ZoneOffset.of("-05:00"));
    private static final OffsetDateTime HASTA = OffsetDateTime.of(2026, 9, 10, 14, 30, 0, 0, ZoneOffset.of("-05:00"));

    private ConductorRepository repositorio;
    private PeticionIdempotenteRepository idempotencia;
    private ConductorInternalService servicio;

    @BeforeEach
    void preparar() {
        repositorio = mock(ConductorRepository.class);
        idempotencia = mock(PeticionIdempotenteRepository.class);
        Clock relojFijo = Clock.fixed(
                HOY.atStartOfDay(ZoneId.of("America/Lima")).toInstant(), ZoneId.of("America/Lima"));
        servicio = new ConductorInternalService(repositorio, idempotencia, relojFijo);
        when(repositorio.save(any(Conductor.class))).thenAnswer(inv -> inv.getArgument(0));
        when(idempotencia.findById(any())).thenReturn(Optional.empty());
    }

    private Conductor conductor(CategoriaDeLicencia categoria, BigDecimal acumuladas, Induccion... inducciones) {
        Conductor c = new Conductor(
                "CON-011", "Juan Perez Vasquez",
                new NumeroDeLicencia("Q12345678"), categoria,
                new PeriodoDeVigencia(LocalDate.of(2025, 1, 1), LocalDate.of(2028, 1, 1)),
                HorasDeConduccion.ventanaDe(HOY),
                EstadoDeHabilitacion.habilitado(),
                List.of(inducciones));
        if (acumuladas.signum() > 0) {
            c.acumularHoras(acumuladas, HOY);
        }
        return c;
    }

    // ---------- Contrato 3 ----------

    @Test
    @DisplayName("la categoria viaja con guion, como la escribe el contrato, no como la escribe el enum")
    void categoriaConGuion() {
        when(repositorio.findById("CON-011"))
                .thenReturn(Optional.of(conductor(CategoriaDeLicencia.A_IIIB, BigDecimal.ZERO)));

        var respuesta = servicio.elegibilidad("CON-011", DESDE, HASTA, TipoDeUnidad.FURGON, null);

        assertThat(respuesta.categoriaLicencia()).isEqualTo("A-IIIB");
        assertThat(respuesta.elegible()).isTrue();
        assertThat(respuesta.motivos()).isEmpty();
    }

    /** La ventana de 8 h 30 min se convierte a 8.50 h, y con 3 ya acumuladas CON-02 no da. */
    @Test
    @DisplayName("los motivos son los codigos exactos del contrato y salen del agregado")
    void motivosDelContrato() {
        Conductor c = conductor(CategoriaDeLicencia.A_IIIA, new BigDecimal("3.00"));
        when(repositorio.findById("CON-011")).thenReturn(Optional.of(c));

        var respuesta = servicio.elegibilidad("CON-011", DESDE, HASTA, TipoDeUnidad.CAMA_BAJA, "CLI-0019");

        assertThat(respuesta.elegible()).isFalse();
        assertThat(respuesta.motivos()).contains(
                "CATEGORIA_INSUFICIENTE", "HORAS_INSUFICIENTES", "INDUCCION_VENCIDA:CLI-0019");
        assertThat(respuesta.horasDisponibles()).isEqualByComparingTo("7.00");
    }

    @Test
    @DisplayName("sin clienteId, CON-03 no se evalua")
    void sinClienteIdNoSeEvaluaLaInduccion() {
        when(repositorio.findById("CON-011"))
                .thenReturn(Optional.of(conductor(CategoriaDeLicencia.A_IIIC, BigDecimal.ZERO)));

        var respuesta = servicio.elegibilidad("CON-011", DESDE, HASTA, TipoDeUnidad.CAMA_BAJA, null);

        assertThat(respuesta.motivos()).noneMatch(m -> m.startsWith("INDUCCION_VENCIDA"));
    }

    @Test
    @DisplayName("un conductor que no existe es 404, no una respuesta de no elegible")
    void conductorInexistente() {
        when(repositorio.findById("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.elegibilidad("no-existe", DESDE, HASTA, TipoDeUnidad.FURGON, null))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ---------- Contrato 6 · idempotencia ----------

    /**
     * La prueba que hace real la regla 6. Sin ella, la idempotencia es una intencion: el reintento
     * debe devolver el resultado original y el agregado tiene que haberse tocado una sola vez.
     */
    @Test
    @DisplayName("el mismo reporte de horas dos veces con la misma clave acumula una sola vez")
    void reintentoNoDuplicaLasHoras() {
        Conductor c = conductor(CategoriaDeLicencia.A_IIIC, BigDecimal.ZERO);
        when(repositorio.findById("CON-011")).thenReturn(Optional.of(c));

        String clave = "VIA-2026-00045:CON-011:horas";
        var peticion = new ReportarHorasRequest("VIA-2026-00045", new BigDecimal("8.50"), DESDE, HASTA);

        var primera = servicio.reportarHoras("CON-011", clave, peticion);
        assertThat(primera.repetida()).isFalse();
        assertThat(primera.cuerpo().horasAcumuladas()).isEqualByComparingTo("8.50");

        // El segundo intento encuentra la clave ya registrada.
        when(idempotencia.findById(clave))
                .thenReturn(Optional.of(new PeticionIdempotente(clave, "CON-011", OffsetDateTime.now())));

        var segunda = servicio.reportarHoras("CON-011", clave, peticion);

        assertThat(segunda.repetida()).isTrue();
        assertThat(segunda.cuerpo().horasAcumuladas()).isEqualByComparingTo("8.50");
        assertThat(c.getHorasAcumuladas().horas()).isEqualByComparingTo("8.50");
        verify(repositorio, times(1)).save(any(Conductor.class));
        verify(idempotencia, times(1)).save(any(PeticionIdempotente.class));
    }

    @Test
    @DisplayName("CON-02 sube sin transformar: el manejador la traduce al 409 que pide el contrato")
    void horasQueSuperanElMaximo() {
        Conductor c = conductor(CategoriaDeLicencia.A_IIIC, new BigDecimal("6.00"));
        when(repositorio.findById("CON-011")).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> servicio.reportarHoras("CON-011", "k-1",
                new ReportarHorasRequest("VIA-1", new BigDecimal("5.00"), DESDE, HASTA)))
                .isInstanceOf(HorasExcedidasException.class);

        verify(idempotencia, never()).save(any(PeticionIdempotente.class));
    }

    @Test
    @DisplayName("la incidencia queda en el legajo y el reintento no la duplica")
    void reintentoNoDuplicaLaIncidencia() {
        Conductor c = conductor(CategoriaDeLicencia.A_IIIC, BigDecimal.ZERO);
        when(repositorio.findById("CON-011")).thenReturn(Optional.of(c));

        String clave = "VIA-2026-00045:CON-011:incidencia";
        var peticion = new ReportarIncidenciaRequest(
                "VIA-2026-00045", "DOCUMENTARIA", "Retencion SUTRAN por guia incompleta.", true);

        var primera = servicio.reportarIncidencia("CON-011", clave, peticion);
        assertThat(primera.repetida()).isFalse();
        assertThat(c.getIncidencias()).hasSize(1);
        assertThat(c.getIncidencias().getFirst().esAtribuible()).isTrue();

        when(idempotencia.findById(clave)).thenReturn(Optional.of(
                new PeticionIdempotente(clave, primera.cuerpo().incidenciaId(), OffsetDateTime.now())));

        var segunda = servicio.reportarIncidencia("CON-011", clave, peticion);

        assertThat(segunda.repetida()).isTrue();
        assertThat(segunda.cuerpo().incidenciaId()).isEqualTo(primera.cuerpo().incidenciaId());
        assertThat(c.getIncidencias()).hasSize(1);
    }

    @Test
    @DisplayName("registrar una incidencia no suspende al conductor: registrar no es sancionar")
    void laIncidenciaNoSuspende() {
        Conductor c = conductor(CategoriaDeLicencia.A_IIIC, BigDecimal.ZERO);
        when(repositorio.findById("CON-011")).thenReturn(Optional.of(c));

        servicio.reportarIncidencia("CON-011", "k-9", new ReportarIncidenciaRequest(
                "VIA-1", "CONDUCTA", "Exceso de velocidad reportado por telemetria.", true));

        assertThat(c.getEstado().estaHabilitado()).isTrue();
    }
}
