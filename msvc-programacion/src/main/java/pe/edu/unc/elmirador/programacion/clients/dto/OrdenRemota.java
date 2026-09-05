package pe.edu.unc.elmirador.programacion.clients.dto;

import java.util.List;

public record OrdenRemota(
    String ordenId,
    String clienteId,
    String estado,
    CargaRemota carga,
    RutaRemota ruta,
    VentanaRemota ventana,
    boolean permiteConsolidacion,
    List<String> restriccionesConsolidacion,
    String tipoUnidadRequerido
) {}
