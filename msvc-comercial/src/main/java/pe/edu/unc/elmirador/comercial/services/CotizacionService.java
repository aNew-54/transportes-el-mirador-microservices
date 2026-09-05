package pe.edu.unc.elmirador.comercial.services;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unc.elmirador.comercial.dto.request.AceptarCotizacionRequest;
import pe.edu.unc.elmirador.comercial.dto.request.EmitirCotizacionRequest;
import pe.edu.unc.elmirador.comercial.dto.request.RechazarCotizacionRequest;
import pe.edu.unc.elmirador.comercial.dto.response.CotizacionResponse;
import pe.edu.unc.elmirador.comercial.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.comercial.mappers.CotizacionMapper;
import pe.edu.unc.elmirador.comercial.models.entity.Cliente;
import pe.edu.unc.elmirador.comercial.models.entity.Cotizacion;
import pe.edu.unc.elmirador.comercial.models.entity.OrdenDeServicio;
import pe.edu.unc.elmirador.comercial.models.entity.Tarifario;
import pe.edu.unc.elmirador.comercial.models.vo.Carga;
import pe.edu.unc.elmirador.comercial.models.vo.CondicionDePago;
import pe.edu.unc.elmirador.comercial.models.vo.Descuento;
import pe.edu.unc.elmirador.comercial.models.vo.Dinero;
import pe.edu.unc.elmirador.comercial.models.vo.PeriodoDeVigencia;
import pe.edu.unc.elmirador.comercial.models.vo.Ruta;
import pe.edu.unc.elmirador.comercial.models.vo.Tarifa;
import pe.edu.unc.elmirador.comercial.repositories.ClienteRepository;
import pe.edu.unc.elmirador.comercial.repositories.CotizacionRepository;
import pe.edu.unc.elmirador.comercial.repositories.OrdenDeServicioRepository;
import pe.edu.unc.elmirador.comercial.repositories.TarifarioRepository;

@Service
public class CotizacionService {

    private final CotizacionRepository cotizacionRepository;
    private final ClienteRepository clienteRepository;
    private final TarifarioRepository tarifarioRepository;
    private final OrdenDeServicioRepository ordenRepository;
    private final Clock reloj;

    public CotizacionService(CotizacionRepository cotizacionRepository, 
                             ClienteRepository clienteRepository, 
                             TarifarioRepository tarifarioRepository, 
                             OrdenDeServicioRepository ordenRepository, 
                             Clock reloj) {
        this.cotizacionRepository = cotizacionRepository;
        this.clienteRepository = clienteRepository;
        this.tarifarioRepository = tarifarioRepository;
        this.ordenRepository = ordenRepository;
        this.reloj = reloj;
    }

    @Transactional
    public CotizacionResponse emitir(EmitirCotizacionRequest peticion) {
        LocalDate hoy = LocalDate.now(reloj);

        Cliente cliente = clienteRepository.findById(peticion.clienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", peticion.clienteId()));

        Tarifario tarifarioVigente = tarifarioRepository.findAll().stream()
                .filter(t -> t.estaVigenteEn(hoy))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Tarifario Vigente", "hoy"));

        Ruta ruta = new Ruta(peticion.rutaOrigen(), peticion.rutaDestino(), peticion.rutaCorredor());
        
        Dinero precioBase = tarifarioVigente.tarifaPara(ruta, peticion.tipoUnidadRequerida())
                .orElseThrow(() -> new RecursoNoEncontradoException("Precio de Tarifario", "ruta/unidad"));

        Descuento descuento = null;
        if (peticion.descuentoPorcentaje() != null) {
            descuento = new Descuento(peticion.descuentoPorcentaje(), peticion.descuentoAutorizadoPor());
        }

        Tarifa tarifa = new Tarifa(precioBase, tarifarioVigente.recargosEstandar(), descuento);
        Carga carga = new Carga(peticion.cargaPesoKg(), peticion.cargaVolumenM3(), peticion.cargaTipo());
        PeriodoDeVigencia vigencia = new PeriodoDeVigencia(hoy, hoy.plusDays(6));

        Cotizacion cotizacion = Cotizacion.emitir(
                UUID.randomUUID().toString(),
                cliente.id(),
                tarifarioVigente.id(),
                carga,
                ruta,
                tarifa,
                vigencia
        );

        return CotizacionMapper.aRespuesta(cotizacionRepository.save(cotizacion));
    }

    @Transactional
    public CotizacionResponse aceptar(String id, AceptarCotizacionRequest peticion) {
        Cotizacion cotizacion = cotizacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cotizacion", id));

        LocalDate hoy = LocalDate.now(reloj);
        cotizacion.aceptar(hoy);

        Cliente cliente = clienteRepository.findById(cotizacion.clienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", cotizacion.clienteId()));

        CondicionDePago condicion = new CondicionDePago(peticion.modalidadDePago(), peticion.plazoEnDias());

        OrdenDeServicio orden = OrdenDeServicio.crear(
                UUID.randomUUID().toString(),
                cliente.id(),
                null, // ContratoId es opcional
                cotizacion.carga(),
                cotizacion.ruta(),
                cotizacion.tarifa(),
                condicion,
                cliente.estadoCrediticio()
        );

        ordenRepository.save(orden);
        return CotizacionMapper.aRespuesta(cotizacionRepository.save(cotizacion));
    }

    @Transactional
    public CotizacionResponse rechazar(String id, RechazarCotizacionRequest peticion) {
        Cotizacion cotizacion = cotizacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cotizacion", id));

        cotizacion.rechazar(peticion.motivo(), LocalDate.now(reloj));

        return CotizacionMapper.aRespuesta(cotizacionRepository.save(cotizacion));
    }
}
