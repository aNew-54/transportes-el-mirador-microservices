package pe.edu.unc.elmirador.programacion.clients.dto;

import java.math.BigDecimal;

public record CargaRemota(
    int pesoKg,
    BigDecimal volumenM3,
    String tipo,
    String embalaje,
    String naturaleza
) {}
