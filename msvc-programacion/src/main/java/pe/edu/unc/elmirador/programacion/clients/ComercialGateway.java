package pe.edu.unc.elmirador.programacion.clients;

import org.springframework.stereotype.Component;
import feign.FeignException;
import feign.RetryableException;
import pe.edu.unc.elmirador.programacion.clients.dto.OrdenRemota;
import pe.edu.unc.elmirador.programacion.exceptions.ComercialIntegrationException;
import pe.edu.unc.elmirador.programacion.models.vo.ClausulaDeConsolidacion;
import pe.edu.unc.elmirador.programacion.models.vo.Ruta;
import pe.edu.unc.elmirador.programacion.models.vo.TipoDeCarga;
import pe.edu.unc.elmirador.programacion.models.vo.VentanaDeTiempo;

@Component
public class ComercialGateway {

    private final ComercialClient cliente;

    public ComercialGateway(ComercialClient cliente) {
        this.cliente = cliente;
    }

    public OrdenConfirmada obtenerOrden(String ordenId) {
        OrdenRemota remoto;
        try {
            remoto = cliente.obtenerOrden(ordenId);
        } catch (RetryableException fallo) {
            throw new ComercialIntegrationException("Comercial no respondio al consultar la orden " + ordenId, fallo);
        } catch (FeignException fallo) {
            throw new ComercialIntegrationException("Comercial respondio " + fallo.status() + " al consultar la orden " + ordenId + ": " + fallo.contentUTF8(), fallo);
        }
        return traducir(ordenId, remoto);
    }
    
    /**
     * El {@code tipo} llega del contrato 1 y no se deduce.
     *
     * <p>Antes se reconstruia a ojo desde {@code embalaje} y {@code naturaleza}, con
     * {@code GENERAL} como salida por defecto para todo lo no reconocido. VIA-05 se decide con ese
     * campo, y la maquinaria pesada es justamente la que no comparte plataforma con nada: una
     * naturaleza escrita de otra forma la convertia en carga general consolidable con cualquier cosa.
     */
    private OrdenConfirmada traducir(String ordenId, OrdenRemota remoto) {
        if (remoto == null || remoto.estado() == null || remoto.carga() == null
                || remoto.ruta() == null || remoto.ventana() == null) {
            throw new ComercialIntegrationException("Comercial respondio una orden incompleta para " + ordenId);
        }

        TipoDeCarga tipo;
        try {
            tipo = TipoDeCarga.valueOf(remoto.carga().tipo());
        } catch (IllegalArgumentException | NullPointerException desconocido) {
            throw new ComercialIntegrationException(
                    "Comercial respondio un tipo de carga que Programacion no conoce: "
                            + remoto.carga().tipo(), desconocido);
        }

        return new OrdenConfirmada(
                remoto.ordenId(),
                remoto.clienteId(),
                remoto.carga().pesoKg(),
                remoto.carga().volumenM3(),
                tipo,
                new Ruta(remoto.ruta().origen(), remoto.ruta().destino(), remoto.ruta().corredor()),
                new VentanaDeTiempo(remoto.ventana().inicio(), remoto.ventana().fin()),
                new ClausulaDeConsolidacion(remoto.permiteConsolidacion(), remoto.restriccionesConsolidacion()),
                remoto.tipoUnidadRequerido());
    }
}
