package pe.edu.unc.elmirador.conductores.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.conductores.exceptions.RehabilitacionInvalidaException;
import pe.edu.unc.elmirador.conductores.models.entity.Conductor;
import pe.edu.unc.elmirador.conductores.models.entity.Induccion;
import pe.edu.unc.elmirador.conductores.models.vo.CategoriaDeLicencia;
import pe.edu.unc.elmirador.conductores.models.vo.EstadoDeHabilitacion;
import pe.edu.unc.elmirador.conductores.models.vo.HorasDeConduccion;
import pe.edu.unc.elmirador.conductores.models.vo.MotivoDeNoElegibilidad;
import pe.edu.unc.elmirador.conductores.models.vo.NumeroDeLicencia;
import pe.edu.unc.elmirador.conductores.models.vo.PeriodoDeVigencia;
import pe.edu.unc.elmirador.conductores.models.vo.TipoDeUnidad;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConductorTest {

    private final LocalDate hoy = LocalDate.of(2026, 9, 10);
    private PeriodoDeVigencia vigenciaValida;
    private PeriodoDeVigencia vigenciaVencida;
    private HorasDeConduccion horasConDisponibilidad;

    @BeforeEach
    void setUp() {
        vigenciaValida = new PeriodoDeVigencia(
                hoy.minusYears(1),
                hoy.plusYears(2)
        );
        vigenciaVencida = new PeriodoDeVigencia(
                hoy.minusYears(2),
                hoy.minusDays(1)
        );
        PeriodoDeVigencia ventana = new PeriodoDeVigencia(hoy, hoy.plusDays(1));
        horasConDisponibilidad = new HorasDeConduccion(new BigDecimal("2.00"), ventana);
    }

    private Conductor crearConductor(
            CategoriaDeLicencia categoria,
            PeriodoDeVigencia vigenciaLicencia,
            EstadoDeHabilitacion estado,
            List<Induccion> inducciones
    ) {
        return new Conductor(
                "CON-001",
                "Juan Perez",
                new NumeroDeLicencia("Q12345678"),
                categoria,
                vigenciaLicencia,
                horasConDisponibilidad,
                estado,
                inducciones
        );
    }

    // ==========================================
    // INVARIANTE CON-01: Licencia y Categoria
    // ==========================================

    @Test
    @DisplayName("CON-01: Conductor con licencia vigente y categoria suficiente es elegible y lista de motivos vacia")
    void conductorConLicenciaVigenteYCategoriaSuficienteEstaHabilitado_CON01() {
        Conductor conductor = crearConductor(
                CategoriaDeLicencia.A_IIIC,
                vigenciaValida,
                EstadoDeHabilitacion.habilitado(),
                Collections.emptyList()
        );

        boolean habilitado = conductor.estaHabilitadoPara(hoy, TipoDeUnidad.CAMA_BAJA, new BigDecimal("3.00"), null);
        List<String> motivos = conductor.motivosDeNoElegibilidad(hoy, TipoDeUnidad.CAMA_BAJA, new BigDecimal("3.00"), null);

        assertThat(habilitado)
                .as("[CON-01] Debe estar habilitado con licencia vigente y categoria A_IIIC para CAMA_BAJA")
                .isTrue();
        assertThat(motivos)
                .as("[CON-01] La lista de motivos debe ser vacia cuando es elegible")
                .isEmpty();
    }

    @Test
    @DisplayName("CON-01: Licencia vencida hace no elegible al conductor con motivo LICENCIA_VENCIDA")
    void licenciaVencidaNoEsElegible_CON01() {
        Conductor conductor = crearConductor(
                CategoriaDeLicencia.A_IIIC,
                vigenciaVencida,
                EstadoDeHabilitacion.habilitado(),
                Collections.emptyList()
        );

        boolean habilitado = conductor.estaHabilitadoPara(hoy, TipoDeUnidad.FURGON, new BigDecimal("2.00"), null);
        List<String> motivos = conductor.motivosDeNoElegibilidad(hoy, TipoDeUnidad.FURGON, new BigDecimal("2.00"), null);

        assertThat(habilitado)
                .as("[CON-01] Licencia vencida debe inhabilitar al conductor")
                .isFalse();
        assertThat(motivos)
                .as("[CON-01] Debe incluir el motivo LICENCIA_VENCIDA")
                .contains(MotivoDeNoElegibilidad.LICENCIA_VENCIDA.codigo());
    }

    @Test
    @DisplayName("CON-01: Categoria insuficiente con licencia vigente hace no elegible con motivo CATEGORIA_INSUFICIENTE")
    void categoriaInsuficienteConLicenciaVigenteNoEsElegible_CON01() {
        // A_IIIA solo habilita FURGON, no PLATAFORMA
        Conductor conductor = crearConductor(
                CategoriaDeLicencia.A_IIIA,
                vigenciaValida,
                EstadoDeHabilitacion.habilitado(),
                Collections.emptyList()
        );

        boolean habilitado = conductor.estaHabilitadoPara(hoy, TipoDeUnidad.PLATAFORMA, new BigDecimal("2.00"), null);
        List<String> motivos = conductor.motivosDeNoElegibilidad(hoy, TipoDeUnidad.PLATAFORMA, new BigDecimal("2.00"), null);

        assertThat(habilitado)
                .as("[CON-01] Categoria A_IIIA no debe habilitar para PLATAFORMA")
                .isFalse();
        assertThat(motivos)
                .as("[CON-01] Debe incluir exclusivamente CATEGORIA_INSUFICIENTE por categoria insuficiente")
                .containsExactly(MotivoDeNoElegibilidad.CATEGORIA_INSUFICIENTE.codigo());
    }

    @Test
    @DisplayName("CON-01: motivosDeNoElegibilidad acumula dos motivos: LICENCIA_VENCIDA y CATEGORIA_INSUFICIENTE")
    void motivosDeNoElegibilidadAcumulaDosMotivosLicenciaVencidaYCategoriaInsuficiente_CON01() {
        // A_IIIA no habilita CAMA_BAJA y la licencia esta vencida
        Conductor conductor = crearConductor(
                CategoriaDeLicencia.A_IIIA,
                vigenciaVencida,
                EstadoDeHabilitacion.habilitado(),
                Collections.emptyList()
        );

        List<String> motivos = conductor.motivosDeNoElegibilidad(hoy, TipoDeUnidad.CAMA_BAJA, new BigDecimal("2.00"), null);

        assertThat(motivos)
                .as("[CON-01] Debe acumular los dos motivos estables del fallo de licencia")
                .containsExactly(
                        MotivoDeNoElegibilidad.LICENCIA_VENCIDA.codigo(),
                        MotivoDeNoElegibilidad.CATEGORIA_INSUFICIENTE.codigo()
                );
    }

    // ==========================================
    // INVARIANTE CON-03: Induccion de Seguridad por Cliente
    // ==========================================

    @Test
    @DisplayName("CON-03: El MISMO conductor es elegible con clienteId nulo y NO elegible con clienteId cuya induccion vencio")
    void mismoConductorElegibleSinClienteIdYNoElegibleConInduccionVencida_CON03() {
        String clienteMinero = "CLI-0019";
        PeriodoDeVigencia induccionVencida = new PeriodoDeVigencia(
                hoy.minusYears(1),
                hoy.minusDays(1)
        );
        Induccion induccion = new Induccion("IND-001", clienteMinero, induccionVencida);

        Conductor conductor = crearConductor(
                CategoriaDeLicencia.A_IIIC,
                vigenciaValida,
                EstadoDeHabilitacion.habilitado(),
                List.of(induccion)
        );

        // Caso 1: clienteId nulo -> CON-03 no se evalua, conductor ES elegible
        boolean elegibleSinCliente = conductor.estaHabilitadoPara(hoy, TipoDeUnidad.FURGON, new BigDecimal("2.00"), null);
        List<String> motivosSinCliente = conductor.motivosDeNoElegibilidad(hoy, TipoDeUnidad.FURGON, new BigDecimal("2.00"), null);

        assertThat(elegibleSinCliente)
                .as("[CON-03] Con clienteId nulo el conductor debe ser elegible")
                .isTrue();
        assertThat(motivosSinCliente)
                .as("[CON-03] Sin cliente exigente la lista de motivos debe ser vacia")
                .isEmpty();

        // Caso 2: clienteId presente con induccion vencida -> conductor NO es elegible
        boolean elegibleConCliente = conductor.estaHabilitadoPara(hoy, TipoDeUnidad.FURGON, new BigDecimal("2.00"), clienteMinero);
        List<String> motivosConCliente = conductor.motivosDeNoElegibilidad(hoy, TipoDeUnidad.FURGON, new BigDecimal("2.00"), clienteMinero);

        assertThat(elegibleConCliente)
                .as("[CON-03] Con induccion vencida para el cliente, el mismo conductor no debe ser elegible")
                .isFalse();
        assertThat(motivosConCliente)
                .as("[CON-03] Debe incluir el motivo INDUCCION_VENCIDA con el clienteId")
                .containsExactly(MotivoDeNoElegibilidad.INDUCCION_VENCIDA.codigo(clienteMinero));
    }

    @Test
    @DisplayName("CON-03: Induccion ausente para el cliente exigido inhabilita con motivo INDUCCION_VENCIDA:<clienteId>")
    void induccionAusenteParaClienteInhabilitaConductor_CON03() {
        Conductor conductor = crearConductor(
                CategoriaDeLicencia.A_IIIC,
                vigenciaValida,
                EstadoDeHabilitacion.habilitado(),
                Collections.emptyList()
        );

        String clienteExigente = "CLI-9999";
        boolean habilitado = conductor.estaHabilitadoPara(hoy, TipoDeUnidad.FURGON, new BigDecimal("2.00"), clienteExigente);
        List<String> motivos = conductor.motivosDeNoElegibilidad(hoy, TipoDeUnidad.FURGON, new BigDecimal("2.00"), clienteExigente);

        assertThat(habilitado)
                .as("[CON-03] Sin induccion registrada para el cliente no debe estar habilitado")
                .isFalse();
        assertThat(motivos)
                .as("[CON-03] Debe reportar INDUCCION_VENCIDA para el cliente ausente")
                .containsExactly(MotivoDeNoElegibilidad.INDUCCION_VENCIDA.codigo(clienteExigente));
    }

    @Test
    @DisplayName("CON-03: Conductor con induccion vigente para el cliente destino es elegible")
    void conductorConInduccionVigenteParaClienteEsElegible_CON03() {
        String clienteMinero = "CLI-0019";
        PeriodoDeVigencia induccionVigente = new PeriodoDeVigencia(
                hoy.minusMonths(2),
                hoy.plusMonths(10)
        );
        Induccion induccion = new Induccion("IND-001", clienteMinero, induccionVigente);

        Conductor conductor = crearConductor(
                CategoriaDeLicencia.A_IIIC,
                vigenciaValida,
                EstadoDeHabilitacion.habilitado(),
                List.of(induccion)
        );

        boolean habilitado = conductor.estaHabilitadoPara(hoy, TipoDeUnidad.FURGON, new BigDecimal("2.00"), clienteMinero);
        List<String> motivos = conductor.motivosDeNoElegibilidad(hoy, TipoDeUnidad.FURGON, new BigDecimal("2.00"), clienteMinero);

        assertThat(habilitado)
                .as("[CON-03] Con induccion vigente debe estar habilitado")
                .isTrue();
        assertThat(motivos).isEmpty();
    }

    @Test
    @DisplayName("CON-03: Registrar una induccion ya vencida no habilita al conductor")
    void registrarInduccionYaVencidaNoHabilita_CON03() {
        Conductor conductor = crearConductor(
                CategoriaDeLicencia.A_IIIC,
                vigenciaValida,
                EstadoDeHabilitacion.habilitado(),
                Collections.emptyList()
        );

        String clienteMinero = "CLI-0019";
        PeriodoDeVigencia vencida = new PeriodoDeVigencia(
                hoy.minusYears(1),
                hoy.minusDays(5)
        );
        Induccion induccionVencida = new Induccion("IND-VENCIDA", clienteMinero, vencida);

        // Registrar induccion ya vencida evaluada contra hoy
        conductor.registrarInduccion(induccionVencida);

        boolean habilitado = conductor.estaHabilitadoPara(hoy, TipoDeUnidad.FURGON, new BigDecimal("2.00"), clienteMinero);
        List<String> motivos = conductor.motivosDeNoElegibilidad(hoy, TipoDeUnidad.FURGON, new BigDecimal("2.00"), clienteMinero);

        assertThat(habilitado)
                .as("[CON-03] Registrar una induccion ya vencida no debe habilitar al conductor")
                .isFalse();
        assertThat(motivos)
                .as("[CON-03] Debe reportar INDUCCION_VENCIDA")
                .contains(MotivoDeNoElegibilidad.INDUCCION_VENCIDA.codigo(clienteMinero));
    }

    @Test
    @DisplayName("Registrar induccion reemplaza la previa del mismo clienteId")
    void registrarInduccionReemplazaPreviaDelMismoCliente() {
        String clienteMinero = "CLI-0019";
        PeriodoDeVigencia vieja = new PeriodoDeVigencia(hoy.minusYears(1), hoy.minusDays(1));
        PeriodoDeVigencia nueva = new PeriodoDeVigencia(hoy, hoy.plusYears(1));

        Induccion ind1 = new Induccion("IND-001", clienteMinero, vieja);
        Conductor conductor = crearConductor(
                CategoriaDeLicencia.A_IIIC,
                vigenciaValida,
                EstadoDeHabilitacion.habilitado(),
                List.of(ind1)
        );

        Induccion ind2 = new Induccion("IND-002", clienteMinero, nueva);
        conductor.registrarInduccion(ind2);

        assertThat(conductor.getInducciones()).hasSize(1);
        assertThat(conductor.getInducciones()).containsExactly(ind2);
        assertThat(conductor.estaHabilitadoPara(hoy, TipoDeUnidad.FURGON, new BigDecimal("2.00"), clienteMinero)).isTrue();
    }

    // ==========================================
    // Estado de habilitacion: suspender / rehabilitar / renovar
    // ==========================================

    @Test
    @DisplayName("Conductor con estado SUSPENDIDO no es elegible y reporta NO_HABILITADO")
    void conductorSuspendidoReportaNoHabilitado() {
        Conductor conductor = crearConductor(
                CategoriaDeLicencia.A_IIIC,
                vigenciaValida,
                EstadoDeHabilitacion.suspendido("Falta disciplinaria grave"),
                Collections.emptyList()
        );

        boolean habilitado = conductor.estaHabilitadoPara(hoy, TipoDeUnidad.FURGON, new BigDecimal("2.00"), null);
        List<String> motivos = conductor.motivosDeNoElegibilidad(hoy, TipoDeUnidad.FURGON, new BigDecimal("2.00"), null);

        assertThat(habilitado).isFalse();
        assertThat(motivos).contains(MotivoDeNoElegibilidad.NO_HABILITADO.codigo());
    }

    @Test
    @DisplayName("suspender cambia el estado a SUSPENDIDO con el motivo indicado")
    void suspenderCambiaEstado() {
        Conductor conductor = crearConductor(
                CategoriaDeLicencia.A_IIIC,
                vigenciaValida,
                EstadoDeHabilitacion.habilitado(),
                Collections.emptyList()
        );

        conductor.suspender("Sancion administrativa");

        assertThat(conductor.getEstado().estaHabilitado()).isFalse();
        assertThat(conductor.getEstado().motivo()).isEqualTo("Sancion administrativa");
    }

    @Test
    @DisplayName("rehabilitar con licencia vencida lanza RehabilitacionInvalidaException")
    void rehabilitarConLicenciaVencidaLanzaRehabilitacionInvalidaException() {
        Conductor conductor = crearConductor(
                CategoriaDeLicencia.A_IIIC,
                vigenciaVencida,
                EstadoDeHabilitacion.suspendido("Sancion temporal"),
                Collections.emptyList()
        );

        assertThatThrownBy(() -> conductor.rehabilitar(hoy))
                .as("Rehabilitar con licencia vencida debe fallar con RehabilitacionInvalidaException")
                .isInstanceOf(RehabilitacionInvalidaException.class)
                .hasMessageContaining("no esta vigente");
    }

    @Test
    @DisplayName("rehabilitar con licencia vigente restablece estado a HABILITADO")
    void rehabilitarConLicenciaVigenteRestableceEstado() {
        Conductor conductor = crearConductor(
                CategoriaDeLicencia.A_IIIC,
                vigenciaValida,
                EstadoDeHabilitacion.suspendido("Sancion temporal"),
                Collections.emptyList()
        );

        conductor.rehabilitar(hoy);

        assertThat(conductor.getEstado().estaHabilitado()).isTrue();
    }

    @Test
    @DisplayName("renovarLicencia reemplaza licencia, categoria y vigencia")
    void renovarLicenciaActualizaDatos() {
        Conductor conductor = crearConductor(
                CategoriaDeLicencia.A_IIIA,
                vigenciaVencida,
                EstadoDeHabilitacion.habilitado(),
                Collections.emptyList()
        );

        NumeroDeLicencia nuevaLic = new NumeroDeLicencia("Q99998888");
        PeriodoDeVigencia nuevaVig = new PeriodoDeVigencia(hoy, hoy.plusYears(3));

        conductor.renovarLicencia(nuevaLic, CategoriaDeLicencia.A_IIIC, nuevaVig);

        assertThat(conductor.getNumeroDeLicencia()).isEqualTo(nuevaLic);
        assertThat(conductor.getCategoriaDeLicencia()).isEqualTo(CategoriaDeLicencia.A_IIIC);
        assertThat(conductor.getVigenciaLicencia()).isEqualTo(nuevaVig);
    }

    // ==========================================
    // REGLA DURA: El dominio no lee el reloj y no evade null
    // ==========================================

    @Test
    @DisplayName("Operaciones con fecha nula lanzan IllegalArgumentException (el dominio no lee el reloj)")
    void operacionesConFechaNulaLanzanExcepcion() {
        Conductor conductor = crearConductor(
                CategoriaDeLicencia.A_IIIC,
                vigenciaValida,
                EstadoDeHabilitacion.habilitado(),
                Collections.emptyList()
        );

        assertThatThrownBy(() -> conductor.estaHabilitadoPara(null, TipoDeUnidad.FURGON, new BigDecimal("2.00"), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> conductor.motivosDeNoElegibilidad(null, TipoDeUnidad.FURGON, new BigDecimal("2.00"), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> conductor.rehabilitar(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> conductor.acumularHoras(new BigDecimal("1.00"), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("motivosDeNoElegibilidad con tipo de unidad u horas requeridas nulas lanza IllegalArgumentException")
    void motivosDeNoElegibilidadConParametrosNulosLanzaExcepcion() {
        Conductor conductor = crearConductor(
                CategoriaDeLicencia.A_IIIC,
                vigenciaValida,
                EstadoDeHabilitacion.habilitado(),
                Collections.emptyList()
        );

        assertThatThrownBy(() -> conductor.motivosDeNoElegibilidad(hoy, null, new BigDecimal("2.00"), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> conductor.motivosDeNoElegibilidad(hoy, TipoDeUnidad.FURGON, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
