package pe.edu.unc.elmirador.comercial.services;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unc.elmirador.comercial.dto.request.CancelarOrdenRequest;
import pe.edu.unc.elmirador.comercial.dto.request.CrearOrdenRequest;
import pe.edu.unc.elmirador.comercial.dto.response.OrdenDeServicioResponse;
import pe.edu.unc.elmirador.comercial.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.comercial.mappers.OrdenDeServicioMapper;
import pe.edu.unc.elmirador.comercial.models.entity.Cliente;
import pe.edu.unc.elmirador.comercial.models.entity.ContratoMarco;
import pe.edu.unc.elmirador.comercial.models.entity.OrdenDeServicio;
import pe.edu.unc.elmirador.comercial.models.vo.Carga;
import pe.edu.unc.elmirador.comercial.models.vo.CondicionDePago;
import pe.edu.unc.elmirador.comercial.models.vo.Dinero;
import pe.edu.unc.elmirador.comercial.models.vo.Ruta;
import pe.edu.unc.elmirador.comercial.models.vo.VentanaDeServicio;
import pe.edu.unc.elmirador.comercial.models.vo.Tarifa;
import pe.edu.unc.elmirador.comercial.repositories.ClienteRepository;
import pe.edu.unc.elmirador.comercial.repositories.ContratoMarcoRepository;
import pe.edu.unc.elmirador.comercial.repositories.OrdenDeServicioRepository;

@Service
public class OrdenDeServicioService {

    private final OrdenDeServicioRepository ordenRepository;
    private final ClienteRepository clienteRepository;
    private final ContratoMarcoRepository contratoRepository;
    private final Clock reloj;

    public OrdenDeServicioService(OrdenDeServicioRepository ordenRepository, 
                                  ClienteRepository clienteRepository, 
                                  ContratoMarcoRepository contratoRepository, 
                                  Clock reloj) {
        this.ordenRepository = ordenRepository;
        this.clienteRepository = clienteRepository;
        this.contratoRepository = contratoRepository;
        this.reloj = reloj;
    }

    @Transactional
    public OrdenDeServicioResponse crear(CrearOrdenRequest peticion) {
        Cliente cliente = clienteRepository.findById(peticion.clienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", peticion.clienteId()));

        ContratoMarco contrato = contratoRepository.findById(peticion.contratoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("ContratoMarco", peticion.contratoId()));

        Ruta ruta = new Ruta(peticion.rutaOrigen(), peticion.rutaDestino(), peticion.rutaCorredor());

        // El precio sale de la tarifa pactada del contrato. CTM-01 vive dentro de tarifaPara: si el
        // contrato no esta vigente en la fecha, devuelve vacio. Un vacio aqui significa que ese
        // contrato no pone precio a esa ruta con esa unidad, y eso es un 404 explicito, no un importe
        // inventado por la capa de aplicacion.
        Dinero precioPactado = contrato
                .tarifaPara(ruta, peticion.tipoUnidad(), LocalDate.now(reloj))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "tarifa pactada para la ruta " + peticion.rutaOrigen() + "-" + peticion.rutaDestino()
                                + " y unidad " + peticion.tipoUnidad() + " en el contrato",
                        peticion.contratoId()));

        OrdenDeServicio orden = OrdenDeServicio.crear(
                UUID.randomUUID().toString(),
                cliente.id(),
                contrato.id(),
                new Carga(peticion.cargaPesoKg(), peticion.cargaVolumenM3(), peticion.cargaTipo(),
                        peticion.cargaEmbalaje(), peticion.cargaNaturaleza()),
                ruta,
                new Tarifa(precioPactado),
                new CondicionDePago(peticion.modalidadDePago(), peticion.plazoEnDias()),
                cliente.estadoCrediticio(),
                new VentanaDeServicio(peticion.ventanaInicio(), peticion.ventanaFin()),
                peticion.tipoUnidad(),
                peticion.rutaDistanciaKm()
        );

        return OrdenDeServicioMapper.aRespuesta(ordenRepository.save(orden));
    }

    @Transactional
    public OrdenDeServicioResponse confirmar(String id) {
        OrdenDeServicio orden = buscar(id);
        orden.confirmar();
        return OrdenDeServicioMapper.aRespuesta(ordenRepository.save(orden));
    }


    @Transactional
    public OrdenDeServicioResponse cancelar(String id, CancelarOrdenRequest peticion) {
        OrdenDeServicio orden = buscar(id);
        orden.cancelar(LocalDate.now(reloj), peticion.autorizadoPor());
        return OrdenDeServicioMapper.aRespuesta(ordenRepository.save(orden));
    }

    @Transactional(readOnly = true)
    public OrdenDeServicioResponse porId(String id) {
        return OrdenDeServicioMapper.aRespuesta(buscar(id));
    }

    private OrdenDeServicio buscar(String id) {
        return ordenRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("OrdenDeServicio", id));
    }
}
