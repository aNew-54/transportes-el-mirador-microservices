package pe.edu.unc.elmirador.ejecucion.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import pe.edu.unc.elmirador.ejecucion.dto.request.AbrirLiquidacionRequest;
import pe.edu.unc.elmirador.ejecucion.dto.response.LiquidacionDeViajeResponse;
import pe.edu.unc.elmirador.ejecucion.models.entity.LiquidacionDeViaje;
import pe.edu.unc.elmirador.ejecucion.models.entity.LiquidacionDeViajeId;
import pe.edu.unc.elmirador.ejecucion.models.vo.EstadoDeLiquidacion;
import pe.edu.unc.elmirador.ejecucion.repositories.LiquidacionDeViajeRepository;

class LiquidacionDeViajeServiceTest {

    private LiquidacionDeViajeRepository repository;
    private Clock clock;
    private LiquidacionDeViajeService service;

    @BeforeEach
    void setUp() {
        repository = mock(LiquidacionDeViajeRepository.class);
        clock = Clock.fixed(Instant.parse("2026-05-01T10:00:00Z"), ZoneId.of("America/Lima"));
        service = new LiquidacionDeViajeService(repository, clock);
    }

    @Test
    @DisplayName("abrir delega al agregado y persiste")
    void abrir() {
        when(repository.existsById(any())).thenReturn(false);

        AbrirLiquidacionRequest request = new AbrirLiquidacionRequest(
                "v-1", "c-1", new BigDecimal("100.00"), "PEN");
        
        LiquidacionDeViajeResponse response = service.abrir(request);
        
        assertThat(response.viajeId()).isEqualTo("v-1");
        assertThat(response.estado()).isEqualTo(EstadoDeLiquidacion.ABIERTA);
        verify(repository).save(any(LiquidacionDeViaje.class));
    }
}
