package pe.edu.unc.elmirador.facturacion.services;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unc.elmirador.facturacion.dto.request.*;
import pe.edu.unc.elmirador.facturacion.dto.response.*;
import pe.edu.unc.elmirador.facturacion.exceptions.*;
import pe.edu.unc.elmirador.facturacion.mappers.FacturaMapper;
import pe.edu.unc.elmirador.facturacion.models.entity.*;
import pe.edu.unc.elmirador.facturacion.models.vo.*;
import pe.edu.unc.elmirador.facturacion.repositories.FacturaRepository;

@Service
public class FacturaService {

    private final FacturaRepository repositorio;
    private final Clock reloj;

    public FacturaService(FacturaRepository repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    @Transactional
    public FacturaResponse abrir(AbrirFacturaRequest req) {
        if (repositorio.existsByOrdenDeServicioId(req.ordenDeServicioId())) {
            throw new ConflictoDeRecursoException("Ya existe una factura para la orden " + req.ordenDeServicioId());
        }

        SnapshotComercial snap = new SnapshotComercial(
            req.ordenDeServicioId(),
            req.clienteId(),
            new Dinero(req.snapshot().tarifaMonto(), req.snapshot().codigoMoneda()),
            req.snapshot().codigoMoneda(),
            req.snapshot().obtenidoEn()
        );

        Detraccion det = req.detraccion().porcentaje().signum() == 0 
            ? Detraccion.sinDetraccion(snap.codigoMoneda()) 
            : new Detraccion(
                req.detraccion().porcentaje(),
                new Dinero(req.detraccion().monto(), snap.codigoMoneda()),
                req.detraccion().cuentaBancaria()
            );

        Factura factura = Factura.abrir(UUID.randomUUID().toString(), snap, det);
        return FacturaMapper.aRespuesta(repositorio.save(factura));
    }

    @Transactional
    public FacturaResponse emitir(String id, EmitirFacturaRequest req) {
        Factura factura = buscar(id);
        factura.emitir(new NumeroDeComprobante(req.serie(), req.correlativo()), OffsetDateTime.now(reloj));
        return FacturaMapper.aRespuesta(repositorio.save(factura));
    }

    @Transactional
    public FacturaResponse anular(String id) {
        Factura factura = buscar(id);
        factura.anular(OffsetDateTime.now(reloj));
        return FacturaMapper.aRespuesta(repositorio.save(factura));
    }

    @Transactional(readOnly = true)
    public FacturaResponse porId(String id) {
        return FacturaMapper.aRespuesta(buscar(id));
    }

    @Transactional(readOnly = true)
    public List<FacturaResponse> listar(EstadoDeFactura estado, String clienteId, LocalDate fecha) {
        List<Factura> facturas = repositorio.findAll();
        if (estado != null) {
            facturas = facturas.stream().filter(f -> f.estado() == estado).toList();
        }
        if (clienteId != null) {
            facturas = facturas.stream().filter(f -> f.clienteId().equals(clienteId)).toList();
        }
        if (fecha != null) {
            facturas = facturas.stream().filter(f -> f.fechaDeEmision() != null && f.fechaDeEmision().atZoneSameInstant(reloj.getZone()).toLocalDate().equals(fecha)).toList();
        }
        return facturas.stream().map(FacturaMapper::aRespuesta).toList();
    }

    @Transactional
    public FacturaResponse emitirFalsoFlete(EmitirFalsoFleteRequest req) {
        if (repositorio.existsByOrdenDeServicioId(req.ordenDeServicioId())) {
            throw new ConflictoDeRecursoException("Ya existe una factura para la orden " + req.ordenDeServicioId());
        }

        SnapshotComercial snap = new SnapshotComercial(
            req.ordenDeServicioId(),
            req.clienteId(),
            new Dinero(req.snapshot().tarifaMonto(), req.snapshot().codigoMoneda()),
            req.snapshot().codigoMoneda(),
            req.snapshot().obtenidoEn()
        );

        Detraccion det = req.detraccion().porcentaje().signum() == 0 
            ? Detraccion.sinDetraccion(snap.codigoMoneda()) 
            : new Detraccion(
                req.detraccion().porcentaje(),
                new Dinero(req.detraccion().monto(), snap.codigoMoneda()),
                req.detraccion().cuentaBancaria()
            );

        Factura factura = Factura.abrirFalsoFlete(UUID.randomUUID().toString(), snap, det);
        factura.agregarLinea(new LineaDeFactura(
            UUID.randomUUID().toString(),
            req.ordenDeServicioId(),
            ConceptoFacturable.FALSO_FLETE,
            req.descripcionLinea(),
            new Dinero(req.importeMonto(), snap.codigoMoneda())
        ));
        factura.emitirFalsoFlete(new NumeroDeComprobante(req.serie(), req.correlativo()), OffsetDateTime.now(reloj));
        return FacturaMapper.aRespuesta(repositorio.save(factura));
    }

    @Transactional
    public void registrarConformidad(RegistrarConformidadRequest req) {
        Factura factura = repositorio.findByOrdenDeServicioId(req.ordenDeServicioId())
            .orElseThrow(() -> new RecursoNoEncontradoException("factura por orden", req.ordenDeServicioId()));
        
        for (LineaDeFacturaRequest lineaReq : req.conceptos()) {
            factura.agregarLinea(new LineaDeFactura(
                UUID.randomUUID().toString(),
                req.ordenDeServicioId(),
                lineaReq.concepto(),
                lineaReq.descripcion(),
                new Dinero(lineaReq.importeMonto(), factura.snapshotComercial().codigoMoneda())
            ));
        }

        factura.registrarConformidad(new Conformidad(req.registrada(), req.incidenciasSinResolver(), req.recibidaEn()));
        repositorio.save(factura);
    }

    private Factura buscar(String id) {
        return repositorio.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("factura", id));
    }
}
