package pe.edu.unc.elmirador.programacion.clients.dto;

public record RutaRemota(
    String origen,
    String destino,
    String corredor,
    int distanciaKm
) {}
