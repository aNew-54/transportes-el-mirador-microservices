package pe.edu.unc.elmirador.cobranza.mappers;

import java.time.LocalDate;
import java.util.List;

import pe.edu.unc.elmirador.cobranza.dto.response.CuentaCorrienteResponse;
import pe.edu.unc.elmirador.cobranza.dto.response.CuentaPorCobrarResponse;
import pe.edu.unc.elmirador.cobranza.dto.response.ImporteResponse;
import pe.edu.unc.elmirador.cobranza.models.entity.CuentaCorrienteDelCliente;
import pe.edu.unc.elmirador.cobranza.models.entity.CuentaPorCobrar;

/**
 * Traduce el agregado a los DTO de respuesta. Va en un solo sentido.
 */
public final class CuentaCorrienteMapper {

    private CuentaCorrienteMapper() {
    }

    public static CuentaCorrienteResponse aRespuesta(CuentaCorrienteDelCliente cuentaCorriente, LocalDate fecha) {
        return new CuentaCorrienteResponse(
                cuentaCorriente.clienteId(),
                cuentaCorriente.estado().situacion(),
                cuentaCorriente.estado().motivo(),
                cuentaCorriente.estado().fechaDeCambio(),
                deudaPorMoneda(cuentaCorriente),
                cuentaCorriente.diasDeAtrasoMaximo(fecha),
                cuentaCorriente.cuentasVencidas(fecha),
                cuentaCorriente.cuentas().stream()
                        .map(c -> aRespuesta(c, fecha))
                        .toList()
        );
    }

    /**
     * Un total por cada moneda que el cliente debe de verdad.
     *
     * <p>Ni las monedas ni los totales los decide este mapeador: {@code monedasConDeuda()} dice
     * cuales hay y {@code deudaTotal(moneda)} suma cada una. Sin cuentas vivas la lista sale vacia,
     * que es la respuesta correcta y no un cero en una moneda inventada.
     */
    private static List<ImporteResponse> deudaPorMoneda(CuentaCorrienteDelCliente cuentaCorriente) {
        return cuentaCorriente.monedasConDeuda().stream()
                .map(cuentaCorriente::deudaTotal)
                .map(deuda -> new ImporteResponse(deuda.monto(), deuda.codigoMoneda()))
                .toList();
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
}
