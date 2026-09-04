package pe.edu.unc.elmirador.conductores.models.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoriaDeLicenciaTest {

    @Test
    @DisplayName("CON-01: A_IIIA habilita para FURGON")
    void aIIiaHabilitaFurgon_CON01() {
        assertThat(CategoriaDeLicencia.A_IIIA.habilitaPara(TipoDeUnidad.FURGON))
                .as("[CON-01] A_IIIA debe habilitar para FURGON")
                .isTrue();
    }

    @Test
    @DisplayName("CON-01: A_IIIA NO habilita para PLATAFORMA")
    void aIIiaNoHabilitaPlataforma_CON01() {
        assertThat(CategoriaDeLicencia.A_IIIA.habilitaPara(TipoDeUnidad.PLATAFORMA))
                .as("[CON-01] A_IIIA no debe habilitar para PLATAFORMA")
                .isFalse();
    }

    @Test
    @DisplayName("CON-01: A_IIIA NO habilita para CAMA_BAJA")
    void aIIiaNoHabilitaCamaBaja_CON01() {
        assertThat(CategoriaDeLicencia.A_IIIA.habilitaPara(TipoDeUnidad.CAMA_BAJA))
                .as("[CON-01] A_IIIA no debe habilitar para CAMA_BAJA")
                .isFalse();
    }

    @Test
    @DisplayName("CON-01: A_IIIB habilita para FURGON")
    void aIIibHabilitaFurgon_CON01() {
        assertThat(CategoriaDeLicencia.A_IIIB.habilitaPara(TipoDeUnidad.FURGON))
                .as("[CON-01] A_IIIB debe habilitar para FURGON")
                .isTrue();
    }

    @Test
    @DisplayName("CON-01: A_IIIB habilita para PLATAFORMA")
    void aIIibHabilitaPlataforma_CON01() {
        assertThat(CategoriaDeLicencia.A_IIIB.habilitaPara(TipoDeUnidad.PLATAFORMA))
                .as("[CON-01] A_IIIB debe habilitar para PLATAFORMA")
                .isTrue();
    }

    @Test
    @DisplayName("CON-01: A_IIIB NO habilita para CAMA_BAJA")
    void aIIibNoHabilitaCamaBaja_CON01() {
        assertThat(CategoriaDeLicencia.A_IIIB.habilitaPara(TipoDeUnidad.CAMA_BAJA))
                .as("[CON-01] A_IIIB no debe habilitar para CAMA_BAJA")
                .isFalse();
    }

    @Test
    @DisplayName("CON-01: A_IIIC habilita para FURGON")
    void aIIicHabilitaFurgon_CON01() {
        assertThat(CategoriaDeLicencia.A_IIIC.habilitaPara(TipoDeUnidad.FURGON))
                .as("[CON-01] A_IIIC debe habilitar para FURGON")
                .isTrue();
    }

    @Test
    @DisplayName("CON-01: A_IIIC habilita para PLATAFORMA")
    void aIIicHabilitaPlataforma_CON01() {
        assertThat(CategoriaDeLicencia.A_IIIC.habilitaPara(TipoDeUnidad.PLATAFORMA))
                .as("[CON-01] A_IIIC debe habilitar para PLATAFORMA")
                .isTrue();
    }

    @Test
    @DisplayName("CON-01: A_IIIC habilita para CAMA_BAJA")
    void aIIicHabilitaCamaBaja_CON01() {
        assertThat(CategoriaDeLicencia.A_IIIC.habilitaPara(TipoDeUnidad.CAMA_BAJA))
                .as("[CON-01] A_IIIC debe habilitar para CAMA_BAJA")
                .isTrue();
    }

    @Test
    @DisplayName("habilitaPara lanza IllegalArgumentException cuando tipoDeUnidad es null")
    void tipoDeUnidadNuloLanzaIllegalArgumentException() {
        assertThatThrownBy(() -> CategoriaDeLicencia.A_IIIA.habilitaPara(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no puede ser nulo");
    }
}
