package pe.edu.unc.elmirador.cobranza.mappers;

import java.time.LocalDate;
import java.util.List;

import pe.edu.unc.elmirador.cobranza.dto.response.CuentaCorrienteResponse;
import pe.edu.unc.elmirador.cobranza.dto.response.CuentaPorCobrarResponse;
import pe.edu.unc.elmirador.cobranza.dto.response.EstadoCrediticioResponse;
import pe.edu.unc.elmirador.cobranza.models.entity.CuentaCorrienteDelCliente;
import pe.edu.unc.elmirador.cobranza.models.entity.CuentaPorCobrar;

public final class CuentaCorrienteMapper {

    private CuentaCorrienteMapper() {
    }

    public static CuentaCorrienteResponse aRespuesta(CuentaCorrienteDelCliente cuentaCorriente, LocalDate fecha) {
        return new CuentaCorrienteResponse(
                cuentaCorriente.clienteId(),
                cuentaCorriente.estado().situacion(),
                cuentaCorriente.estado().motivo(),
                cuentaCorriente.estado().fechaDeCambio(),
                cuentaCorriente.deudaTotal("PEN").monto(), // Default PEN for deudaTotal response if no parameter
                cuentaCorriente.deudaTotal("PEN").codigoMoneda(),
                cuentaCorriente.diasDeAtrasoMaximo(fecha),
                cuentaCorriente.cuentasVencidas(fecha),
                cuentaCorriente.cuentas().stream()
                        .map(c -> aRespuesta(c, fecha))
                        .toList()
        );
    }
    
    public static CuentaCorrienteResponse aRespuesta(CuentaCorrienteDelCliente cuentaCorriente, LocalDate fecha, String moneda) {
        return new CuentaCorrienteResponse(
                cuentaCorriente.clienteId(),
                cuentaCorriente.estado().situacion(),
                cuentaCorriente.estado().motivo(),
                cuentaCorriente.estado().fechaDeCambio(),
                cuentaCorriente.deudaTotal(moneda).monto(),
                cuentaCorriente.deudaTotal(moneda).codigoMoneda(),
                cuentaCorriente.diasDeAtrasoMaximo(fecha),
                cuentaCorriente.cuentasVencidas(fecha),
                cuentaCorriente.cuentas().stream()
                        .map(c -> aRespuesta(c, fecha))
                        .toList()
        );
    }

    public static CuentaPorCobrarResponse aRespuesta(CuentaPorCobrar cuenta, LocalDate fecha) {
        return new CuentaPorCobrarResponse(
                cuenta.id(),
                cuenta.clienteId(),
                cuenta.facturaId(),
                cuenta.documentoId(),
                cuenta.total().monto(),
                cuenta.total().codigoMoneda(),
                cuenta.detraccion().monto(),
                cuenta.detraccion().codigoMoneda(),
                cuenta.aplicado().monto(),
                cuenta.aplicado().codigoMoneda(),
                cuenta.montoNeto().monto(),
                cuenta.montoNeto().codigoMoneda(),
                cuenta.saldo().monto(),
                cuenta.saldo().codigoMoneda(),
                cuenta.fechaDeVencimiento(),
                cuenta.detraccionDepositada(),
                cuenta.estadoEn(fecha),
                cuenta.diasDeAtraso(fecha).dias()
        );
    }

    public static EstadoCrediticioResponse aEstadoCrediticioRespuesta(CuentaCorrienteDelCliente cuentaCorriente) {
        return new EstadoCrediticioResponse(
                cuentaCorriente.estado().situacion(),
                cuentaCorriente.estado().motivo(),
                cuentaCorriente.estado().fechaDeCambio()
        );
    }
}
