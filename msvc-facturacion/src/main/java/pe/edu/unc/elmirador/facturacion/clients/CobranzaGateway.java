package pe.edu.unc.elmirador.facturacion.clients;

import org.springframework.stereotype.Component;
import org.springframework.http.ResponseEntity;
import feign.FeignException;
import feign.RetryableException;
import pe.edu.unc.elmirador.facturacion.clients.dto.CuentaPorCobrarCreadaRemoto;
import pe.edu.unc.elmirador.facturacion.clients.dto.CuentaPorCobrarRequestRemoto;
import pe.edu.unc.elmirador.facturacion.exceptions.CobranzaIntegrationException;
import pe.edu.unc.elmirador.facturacion.models.entity.Factura;
import pe.edu.unc.elmirador.facturacion.models.vo.SnapshotComercial;

import java.time.OffsetDateTime;

@Component
public class CobranzaGateway {

    private final CobranzaClient cliente;

    public CobranzaGateway(CobranzaClient cliente) {
        this.cliente = cliente;
    }

    public void crearCuentaPorCobrar(Factura factura) {
        SnapshotComercial snapshot = factura.snapshotComercial();
        if (!"CREDITO".equalsIgnoreCase(snapshot.condicionDePagoModalidad())) {
            return;
        }

        CuentaPorCobrarRequestRemoto peticion = new CuentaPorCobrarRequestRemoto(
                factura.id(),
                factura.numeroDeComprobante() != null ? factura.numeroDeComprobante().serie() + "-" + String.format("%08d", factura.numeroDeComprobante().correlativo()) : "SIN-COMPROBANTE",
                factura.clienteId(),
                new CuentaPorCobrarRequestRemoto.ImporteRemoto(factura.total().monto().toString(), factura.total().codigoMoneda()),
                new CuentaPorCobrarRequestRemoto.DetraccionRemoto(
                        factura.detraccion().porcentaje(),
                        factura.detraccion().monto().monto().toString(),
                        factura.detraccion().monto().codigoMoneda(),
                        factura.detraccion().cuentaBancaria()
                ),
                new CuentaPorCobrarRequestRemoto.ImporteRemoto(factura.montoNeto().monto().toString(), factura.montoNeto().codigoMoneda()),
                factura.fechaDeEmision(),
                factura.fechaDeEmision().plusDays(snapshot.condicionDePagoPlazo()),
                new CuentaPorCobrarRequestRemoto.CondicionDePagoRemota(
                        snapshot.condicionDePagoModalidad(),
                        snapshot.condicionDePagoPlazo()
                )
        );

        String idempotencyKey = factura.id();

        try {
            ResponseEntity<CuentaPorCobrarCreadaRemoto> respuesta = cliente.crearCuentaPorCobrar(idempotencyKey, peticion);
            if (respuesta.getStatusCode().is2xxSuccessful()) {
                return;
            }
        } catch (RetryableException fallo) {
            throw new CobranzaIntegrationException(
                    "Cobranza no respondio al crear la cuenta por cobrar para la factura " + factura.id(), fallo);
        } catch (FeignException fallo) {
            throw new CobranzaIntegrationException(
                    "Cobranza respondio " + fallo.status() + " al crear la cuenta por cobrar para la factura " + factura.id(), fallo);
        }
    }
}
