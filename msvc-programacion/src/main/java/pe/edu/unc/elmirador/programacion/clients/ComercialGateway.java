package pe.edu.unc.elmirador.programacion.clients;

import org.springframework.stereotype.Component;
import feign.FeignException;
import feign.RetryableException;
import pe.edu.unc.elmirador.programacion.clients.dto.OrdenRemota;
import pe.edu.unc.elmirador.programacion.exceptions.ComercialIntegrationException;
import pe.edu.unc.elmirador.programacion.models.vo.Carga;
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
            throw new ComercialIntegrationException("Comercial respondio " + fallo.status() + " al consultar la orden " + ordenId, fallo);
        }
        return traducir(ordenId, remoto);
    }
    
    private OrdenConfirmada traducir(String ordenId, OrdenRemota remoto) {
        if (remoto == null || remoto.estado() == null || remoto.carga() == null || remoto.ruta() == null || remoto.ventana() == null) {
            throw new ComercialIntegrationException("Comercial respondio una orden incompleta para " + ordenId);
        }
        
        TipoDeCarga tipo;
        try {
            if ("PALLETS".equalsIgnoreCase(remoto.carga().embalaje())) {
                tipo = TipoDeCarga.PALETIZADA;
            } else if ("MAQUINARIA".equalsIgnoreCase(remoto.carga().naturaleza()) || "MAQUINARIA_PESADA".equalsIgnoreCase(remoto.carga().naturaleza())) {
                tipo = TipoDeCarga.MAQUINARIA_PESADA;
            } else {
                tipo = TipoDeCarga.GENERAL;
            }
        } catch (Exception e) {
            throw new ComercialIntegrationException("Comercial respondio una carga que no se puede traducir para " + ordenId, e);
        }
        
        Carga carga = new Carga(remoto.ordenId(), remoto.carga().pesoKg(), remoto.carga().volumenM3(), tipo, 1);
        Ruta ruta = new Ruta(remoto.ruta().origen(), remoto.ruta().destino(), remoto.ruta().corredor());
        VentanaDeTiempo ventana = new VentanaDeTiempo(remoto.ventana().inicio(), remoto.ventana().fin());
        ClausulaDeConsolidacion clausula = new ClausulaDeConsolidacion(remoto.permiteConsolidacion(), remoto.restriccionesConsolidacion());
        
        return new OrdenConfirmada(remoto.ordenId(), remoto.clienteId(), carga, ruta, ventana, clausula, remoto.tipoUnidadRequerido());
    }
}
