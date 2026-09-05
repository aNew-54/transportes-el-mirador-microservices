package pe.edu.unc.elmirador.programacion.clients;

import java.math.BigDecimal;

import pe.edu.unc.elmirador.programacion.models.vo.Carga;
import pe.edu.unc.elmirador.programacion.models.vo.ClausulaDeConsolidacion;
import pe.edu.unc.elmirador.programacion.models.vo.Ruta;
import pe.edu.unc.elmirador.programacion.models.vo.TipoDeCarga;
import pe.edu.unc.elmirador.programacion.models.vo.VentanaDeTiempo;

/**
 * Lo que el contrato 1 dice de una orden confirmada, ya traducido al idioma de Programacion.
 *
 * <p>No trae una {@link Carga} montada, sino sus piezas. La {@code secuenciaDeDescarga} que a Carga
 * le falta no es un dato de Comercial: es donde Programacion decide poner esta orden dentro de la
 * estiba del viaje, y {@code CargaConsolidada} ordena por ella. Comercial no sabe con que otras
 * ordenes va a compartir plataforma esta.
 */
public record OrdenConfirmada(
        String ordenId,
        String clienteId,
        int pesoKg,
        BigDecimal volumenM3,
        TipoDeCarga tipo,
        Ruta ruta,
        VentanaDeTiempo ventana,
        ClausulaDeConsolidacion clausula,
        String tipoUnidadRequerido
) {

    public Carga cargaCon(int secuenciaDeDescarga) {
        return new Carga(ordenId, pesoKg, volumenM3, tipo, secuenciaDeDescarga);
    }
}
