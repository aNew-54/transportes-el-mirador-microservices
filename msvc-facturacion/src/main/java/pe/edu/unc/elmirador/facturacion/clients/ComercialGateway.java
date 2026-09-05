package pe.edu.unc.elmirador.facturacion.clients;

import org.springframework.stereotype.Component;

import feign.FeignException;
import feign.RetryableException;
import pe.edu.unc.elmirador.facturacion.clients.dto.SnapshotFacturableRemoto;
import pe.edu.unc.elmirador.facturacion.exceptions.ComercialIntegrationException;
import pe.edu.unc.elmirador.facturacion.models.vo.Dinero;
import pe.edu.unc.elmirador.facturacion.models.vo.SnapshotComercial;
import java.math.BigDecimal;

@Component
public class ComercialGateway {

    private final ComercialClient cliente;

    public ComercialGateway(ComercialClient cliente) {
        this.cliente = cliente;
    }

    public SnapshotComercial snapshotFacturableDe(String ordenId) {
        SnapshotFacturableRemoto remoto;
        try {
            remoto = cliente.snapshotFacturableDe(ordenId);
        } catch (RetryableException fallo) {
            throw new ComercialIntegrationException(
                    "Comercial no respondio al consultar el snapshot de la orden " + ordenId, fallo);
        } catch (FeignException fallo) {
            throw new ComercialIntegrationException(
                    "Comercial respondio " + fallo.status() + " al consultar el snapshot de la orden " + ordenId, fallo);
        }
        return traducir(ordenId, remoto);
    }

    private SnapshotComercial traducir(String ordenId, SnapshotFacturableRemoto remoto) {
        if (remoto == null || remoto.tarifa() == null || remoto.tarifa().total() == null || remoto.condicionDePago() == null) {
            throw new ComercialIntegrationException(
                    "Comercial respondio un snapshot incompleto para la orden " + ordenId);
        }
        try {
            BigDecimal monto = new BigDecimal(remoto.tarifa().total().monto());
            Dinero tarifa = new Dinero(monto, remoto.tarifa().total().moneda());
            return new SnapshotComercial(
                    remoto.ordenId(),
                    remoto.clienteId(),
                    tarifa,
                    remoto.tarifa().total().moneda(),
                    remoto.tomadoEn(),
                    remoto.condicionDePago().modalidad(),
                    remoto.condicionDePago().plazoEnDias()
            );
        } catch (RuntimeException desconocida) {
            throw new ComercialIntegrationException(
                    "Comercial respondio un snapshot que Facturacion no pudo decodificar", desconocida);
        }
    }
}
