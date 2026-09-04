package pe.edu.unc.elmirador.unidades.models.vo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TipoDeUnidadTest {

    // --- 9 combinaciones de admite ---

    @Test
    @DisplayName("FURGON admite carga PALETIZADA")
    void furgonAdmiteCargaPaletizada() {
        assertThat(TipoDeUnidad.FURGON.admite(TipoDeCarga.PALETIZADA)).isTrue();
    }

    @Test
    @DisplayName("FURGON admite carga GENERAL")
    void furgonAdmiteCargaGeneral() {
        assertThat(TipoDeUnidad.FURGON.admite(TipoDeCarga.GENERAL)).isTrue();
    }

    @Test
    @DisplayName("FURGON NO admite MAQUINARIA_PESADA")
    void furgonNoAdmiteMaquinariaPesada() {
        assertThat(TipoDeUnidad.FURGON.admite(TipoDeCarga.MAQUINARIA_PESADA)).isFalse();
    }

    @Test
    @DisplayName("PLATAFORMA admite carga PALETIZADA")
    void plataformaAdmiteCargaPaletizada() {
        assertThat(TipoDeUnidad.PLATAFORMA.admite(TipoDeCarga.PALETIZADA)).isTrue();
    }

    @Test
    @DisplayName("PLATAFORMA admite carga GENERAL")
    void plataformaAdmiteCargaGeneral() {
        assertThat(TipoDeUnidad.PLATAFORMA.admite(TipoDeCarga.GENERAL)).isTrue();
    }

    @Test
    @DisplayName("PLATAFORMA NO admite MAQUINARIA_PESADA")
    void plataformaNoAdmiteMaquinariaPesada() {
        assertThat(TipoDeUnidad.PLATAFORMA.admite(TipoDeCarga.MAQUINARIA_PESADA)).isFalse();
    }

    @Test
    @DisplayName("CAMA_BAJA NO admite carga PALETIZADA")
    void camaBajaNoAdmiteCargaPaletizada() {
        assertThat(TipoDeUnidad.CAMA_BAJA.admite(TipoDeCarga.PALETIZADA)).isFalse();
    }

    @Test
    @DisplayName("CAMA_BAJA admite carga GENERAL")
    void camaBajaAdmiteCargaGeneral() {
        assertThat(TipoDeUnidad.CAMA_BAJA.admite(TipoDeCarga.GENERAL)).isTrue();
    }

    @Test
    @DisplayName("CAMA_BAJA admite MAQUINARIA_PESADA")
    void camaBajaAdmiteMaquinariaPesada() {
        assertThat(TipoDeUnidad.CAMA_BAJA.admite(TipoDeCarga.MAQUINARIA_PESADA)).isTrue();
    }

    // --- 3 tipos de licenciaRequerida ---

    @Test
    @DisplayName("FURGON requiere licencia A_IIIA")
    void furgonRequiereLicenciaA_IIIA() {
        assertThat(TipoDeUnidad.FURGON.licenciaRequerida()).isEqualTo(CategoriaDeLicencia.A_IIIA);
    }

    @Test
    @DisplayName("PLATAFORMA requiere licencia A_IIIB")
    void plataformaRequiereLicenciaA_IIIB() {
        assertThat(TipoDeUnidad.PLATAFORMA.licenciaRequerida()).isEqualTo(CategoriaDeLicencia.A_IIIB);
    }

    @Test
    @DisplayName("CAMA_BAJA requiere licencia A_IIIC")
    void camaBajaRequiereLicenciaA_IIIC() {
        assertThat(TipoDeUnidad.CAMA_BAJA.licenciaRequerida()).isEqualTo(CategoriaDeLicencia.A_IIIC);
    }
}
