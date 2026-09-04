package pe.edu.unc.elmirador.facturacion.models.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.facturacion.exceptions.NumeroDeComprobanteInvalidoException;

class NumeroDeComprobanteTest {

    @Test
    @DisplayName("NumeroDeComprobante: serie valida F001 o B001 con correlativo positivo se crea correctamente")
    void debeCrearNumeroDeComprobanteValido() {
        NumeroDeComprobante factura = new NumeroDeComprobante("F001", 310);
        assertThat(factura.serie()).isEqualTo("F001");
        assertThat(factura.correlativo()).isEqualTo(310);

        NumeroDeComprobante boleta = new NumeroDeComprobante("B002", 1);
        assertThat(boleta.serie()).isEqualTo("B002");
        assertThat(boleta.correlativo()).isEqualTo(1);
    }

    @Test
    @DisplayName("NumeroDeComprobante: con serie F01, FF001 o correlativo 0 lanza NumeroDeComprobanteInvalidoException")
    void debeRechazarSeriesInvalidasOCorrelativoCero() {
        assertThatThrownBy(() -> new NumeroDeComprobante("F01", 1))
            .isInstanceOf(NumeroDeComprobanteInvalidoException.class)
            .hasMessageContaining("La serie debe iniciar con F o B seguido de 3 digitos");

        assertThatThrownBy(() -> new NumeroDeComprobante("FF001", 1))
            .isInstanceOf(NumeroDeComprobanteInvalidoException.class)
            .hasMessageContaining("La serie debe iniciar con F o B seguido de 3 digitos");

        assertThatThrownBy(() -> new NumeroDeComprobante("F001", 0))
            .isInstanceOf(NumeroDeComprobanteInvalidoException.class)
            .hasMessageContaining("El correlativo debe ser positivo");

        assertThatThrownBy(() -> new NumeroDeComprobante("F001", -5))
            .isInstanceOf(NumeroDeComprobanteInvalidoException.class)
            .hasMessageContaining("El correlativo debe ser positivo");
    }

    @Test
    @DisplayName("NumeroDeComprobante: formateado rellena el correlativo a ocho digitos")
    void debeFormatearCorrelativoAOchoDigitos() {
        NumeroDeComprobante comprobante = new NumeroDeComprobante("F001", 310);
        assertThat(comprobante.formateado()).isEqualTo("F001-00000310");

        NumeroDeComprobante comprobante1 = new NumeroDeComprobante("B001", 1);
        assertThat(comprobante1.formateado()).isEqualTo("B001-00000001");
    }

    @Test
    @DisplayName("NumeroDeComprobante: siguiente devuelve el correlativo incrementado en uno dentro de la misma serie")
    void debeObtenerSiguienteComprobante() {
        NumeroDeComprobante actual = new NumeroDeComprobante("F001", 310);
        NumeroDeComprobante siguiente = actual.siguiente();

        assertThat(siguiente.serie()).isEqualTo("F001");
        assertThat(siguiente.correlativo()).isEqualTo(311);
        assertThat(siguiente.formateado()).isEqualTo("F001-00000311");
    }

    @Test
    @DisplayName("NumeroDeComprobante: factory de(String) parsea correctamente serie y correlativo")
    void debeParsearDesdeCadenaFormateada() {
        NumeroDeComprobante comprobante = NumeroDeComprobante.de("F001-00000310");
        assertThat(comprobante.serie()).isEqualTo("F001");
        assertThat(comprobante.correlativo()).isEqualTo(310);

        assertThatThrownBy(() -> NumeroDeComprobante.de("F001"))
            .isInstanceOf(NumeroDeComprobanteInvalidoException.class);
    }
}
