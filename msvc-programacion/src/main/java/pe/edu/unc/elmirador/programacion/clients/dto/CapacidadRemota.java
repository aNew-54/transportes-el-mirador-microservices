package pe.edu.unc.elmirador.programacion.clients.dto;

import java.math.BigDecimal;

public record CapacidadRemota(
    int pesoMaximoKg,
    BigDecimal volumenMaximoM3
) {}
