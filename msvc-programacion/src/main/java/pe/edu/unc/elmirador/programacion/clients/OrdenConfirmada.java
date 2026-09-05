package pe.edu.unc.elmirador.programacion.clients;

import pe.edu.unc.elmirador.programacion.models.vo.Carga;
import pe.edu.unc.elmirador.programacion.models.vo.ClausulaDeConsolidacion;
import pe.edu.unc.elmirador.programacion.models.vo.Ruta;
import pe.edu.unc.elmirador.programacion.models.vo.TipoDeCarga;
import pe.edu.unc.elmirador.programacion.models.vo.VentanaDeTiempo;

public record OrdenConfirmada(
        String ordenId,
        String clienteId,
        Carga carga,
        Ruta ruta,
        VentanaDeTiempo ventana,
        ClausulaDeConsolidacion clausula,
        String tipoUnidadRequerido
) {}
