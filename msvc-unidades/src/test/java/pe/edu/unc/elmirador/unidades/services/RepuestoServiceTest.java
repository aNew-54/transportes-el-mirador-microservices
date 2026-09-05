package pe.edu.unc.elmirador.unidades.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import pe.edu.unc.elmirador.unidades.dto.request.AjustarInventarioRequest;
import pe.edu.unc.elmirador.unidades.dto.response.RepuestoResponse;
import pe.edu.unc.elmirador.unidades.exceptions.ExistenciasNegativasException;
import pe.edu.unc.elmirador.unidades.models.entity.Repuesto;
import pe.edu.unc.elmirador.unidades.models.vo.Dinero;
import pe.edu.unc.elmirador.unidades.repositories.RepuestoRepository;

class RepuestoServiceTest {

    private RepuestoRepository repositorio;
    private RepuestoService servicio;

    @BeforeEach
    void setUp() {
        repositorio = mock(RepuestoRepository.class);
        servicio = new RepuestoService(repositorio);
    }

    @Test
    @DisplayName("ajustarInventario actualiza existencias")
    void ajustarInventario() {
        Repuesto repuesto = new Repuesto("r-1", "F-001", "Filtro", 10, 5, new Dinero(BigDecimal.TEN, "PEN"));
        when(repositorio.findById("r-1")).thenReturn(Optional.of(repuesto));
        when(repositorio.save(any(Repuesto.class))).thenAnswer(i -> i.getArgument(0));

        AjustarInventarioRequest request = new AjustarInventarioRequest(-5);
        RepuestoResponse response = servicio.ajustarInventario("r-1", request);

        assertThat(response.existencias()).isEqualTo(5);
        verify(repositorio).save(any(Repuesto.class));
    }

    @Test
    @DisplayName("ajustarInventario con negativo excesivo lanza excepcion del dominio sin transformar")
    void ajustarInventarioExcesivo() {
        Repuesto repuesto = new Repuesto("r-1", "F-001", "Filtro", 10, 5, new Dinero(BigDecimal.TEN, "PEN"));
        when(repositorio.findById("r-1")).thenReturn(Optional.of(repuesto));

        AjustarInventarioRequest request = new AjustarInventarioRequest(-15);

        Throwable error = catchThrowable(() -> servicio.ajustarInventario("r-1", request));

        assertThat(error).isInstanceOf(ExistenciasNegativasException.class);
    }
}
