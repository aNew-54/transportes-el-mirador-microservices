package pe.edu.unc.elmirador.comercial.services;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unc.elmirador.comercial.clients.CobranzaGateway;
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
import pe.edu.unc.elmirador.comercial.models.vo.EstadoCrediticio;
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
    private final CobranzaGateway cobranza;
    private final Clock reloj;

    public OrdenDeServicioService(OrdenDeServicioRepository ordenRepository,
                                  ClienteRepository clienteRepository,
                                  ContratoMarcoRepository contratoRepository,
                                  CobranzaGateway cobranza,
                                  Clock reloj) {
        this.ordenRepository = ordenRepository;
        this.clienteRepository = clienteRepository;
        this.contratoRepository = contratoRepository;
        this.cobranza = cobranza;
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

        CondicionDePago condicion = new CondicionDePago(peticion.modalidadDePago(), peticion.plazoEnDias());

        OrdenDeServicio orden = OrdenDeServicio.crear(
                UUID.randomUUID().toString(),
                cliente.id(),
                contrato.id(),
                new Carga(peticion.cargaPesoKg(), peticion.cargaVolumenM3(), peticion.cargaTipo(),
                        peticion.cargaEmbalaje(), peticion.cargaNaturaleza()),
                ruta,
                new Tarifa(precioPactado),
                condicion,
                estadoCrediticioPara(cliente, condicion),
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

    /**
     * Contrato 11. El estado crediticio que decide ORD-02 lo pone Cobranza, que es la fuente de verdad,
     * y solo se le pregunta cuando la condicion lo exige: una orden al contado tiene que poder crearse
     * con Cobranza caida, y eso es la segunda mitad de CLI-01.
     *
     * <p>De paso se refresca la copia local, que existe para las ordenes al contado y para no quedarse
     * sin nada cuando nunca se ha consultado. La decision se toma con lo leido, no con lo guardado.
     *
     * <p>Si Cobranza no responde, {@link CobranzaGateway} lanza y esto no devuelve nada: la orden a
     * credito se rechaza con un 503. No se asume VIGENTE.
     */
    private EstadoCrediticio estadoCrediticioPara(Cliente cliente, CondicionDePago condicion) {
        if (!condicion.exigeVerificacionCrediticia()) {
            return cliente.estadoCrediticio();
        }
        EstadoCrediticio lectura = cobranza.estadoCrediticioDe(cliente.id());
        cliente.refrescarSiEsMasReciente(lectura);
        clienteRepository.save(cliente);
        return lectura;
    }

    private OrdenDeServicio buscar(String id) {
        return ordenRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("OrdenDeServicio", id));
    }
}
