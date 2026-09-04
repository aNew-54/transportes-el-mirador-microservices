package pe.edu.unc.elmirador.ejecucion.models.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvidenciaTest {

    private static final ZoneOffset LIMA = ZoneOffset.ofHours(-5);
    private static final OffsetDateTime AHORA = OffsetDateTime.of(2026, 9, 10, 10, 0, 0, 0, LIMA);

    @Test
    @DisplayName("Crea evidencia valida con fotografias inmutables y descripcion correcta")
    void crearEvidenciaValida() {
        List<String> fotos = List.of("https://storage.elmirador.pe/evidencias/foto1.jpg");
        Evidencia evidencia = new Evidencia(fotos, "Carga con embalaje roto", AHORA);

        assertThat(evidencia.fotografias()).containsExactly("https://storage.elmirador.pe/evidencias/foto1.jpg");
        assertThat(evidencia.descripcion()).isEqualTo("Carga con embalaje roto");
        assertThat(evidencia.momento()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Lista de fotografias es inmutable y no puede alterarse externamente")
    void listaDeFotografiasEsInmutable() {
        List<String> listaModificable = new ArrayList<>();
        listaModificable.add("https://storage.elmirador.pe/evidencias/foto1.jpg");

        Evidencia evidencia = new Evidencia(listaModificable, "Carga mojada", AHORA);
        listaModificable.add("https://storage.elmirador.pe/evidencias/foto2.jpg");

        assertThat(evidencia.fotografias()).hasSize(1);

        assertThatThrownBy(() -> evidencia.fotografias().add("otra.jpg"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Evidencia sin fotografias lanza IllegalArgumentException")
    void evidenciaSinFotografiasLanzaExcepcion() {
        assertThatThrownBy(() -> new Evidencia(null, "Descripcion", AHORA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("al menos una fotografia");

        assertThatThrownBy(() -> new Evidencia(List.of(), "Descripcion", AHORA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("al menos una fotografia");
    }

    @Test
    @DisplayName("Evidencia con fotografias en blanco o nulas lanza IllegalArgumentException")
    void fotografiaEnBlancoLanzaExcepcion() {
        List<String> fotosConBlanco = new ArrayList<>();
        fotosConBlanco.add("   ");

        assertThatThrownBy(() -> new Evidencia(fotosConBlanco, "Descripcion", AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Evidencia con descripcion vacia o nula lanza IllegalArgumentException")
    void descripcionVaciaLanzaExcepcion() {
        List<String> fotos = List.of("https://storage.elmirador.pe/evidencias/foto1.jpg");

        assertThatThrownBy(() -> new Evidencia(fotos, null, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Evidencia(fotos, "   ", AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Evidencia con momento nulo lanza IllegalArgumentException (D1)")
    void momentoNuloLanzaExcepcion() {
        List<String> fotos = List.of("https://storage.elmirador.pe/evidencias/foto1.jpg");

        assertThatThrownBy(() -> new Evidencia(fotos, "Descripcion valida", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("momento");
    }
}
