package pe.edu.unc.elmirador.cobranza.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import pe.edu.unc.elmirador.cobranza.models.vo.EstadoDeDocumento;

public record CuentaPorCobrarResponse(
        String id,
        String clienteId,
        String facturaId,
        String documentoId,
        BigDecimal totalMonto,
        String totalMoneda,
        BigDecimal detraccionMonto,
        String detraccionMoneda,
        BigDecimal aplicadoMonto,
        String aplicadoMoneda,
        BigDecimal montoNetoMonto,
        String montoNetoMoneda,
        BigDecimal saldoMonto,
        String saldoMoneda,
        LocalDate fechaDeVencimiento,
        boolean detraccionDepositada,
        EstadoDeDocumento estado,
        int diasDeAtraso
) {
}
