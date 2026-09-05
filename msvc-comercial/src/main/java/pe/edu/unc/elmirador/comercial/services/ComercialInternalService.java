package pe.edu.unc.elmirador.comercial.services;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unc.elmirador.comercial.dto.internal.request.DiferenciaDeCargaRequest;
import pe.edu.unc.elmirador.comercial.dto.internal.request.EsperaRequest;
import pe.edu.unc.elmirador.comercial.dto.internal.response.DiferenciaRegistradaResponse;
import pe.edu.unc.elmirador.comercial.dto.internal.response.EsperaRegistradaResponse;
import pe.edu.unc.elmirador.comercial.dto.internal.response.OrdenConfirmadaResponse;
import pe.edu.unc.elmirador.comercial.dto.internal.response.SnapshotFacturableResponse;
import pe.edu.unc.elmirador.comercial.dto.response.ResultadoIdempotente;
import pe.edu.unc.elmirador.comercial.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.comercial.exceptions.TransicionDeOrdenInvalidaException;
import pe.edu.unc.elmirador.comercial.models.entity.Cliente;
import pe.edu.unc.elmirador.comercial.models.entity.ContratoMarco;
import java.util.UUID;
import pe.edu.unc.elmirador.comercial.models.entity.EsperaRegistrada;
import pe.edu.unc.elmirador.comercial.models.vo.Carga;
import pe.edu.unc.elmirador.comercial.models.vo.ClausulaDeConsolidacion;
import pe.edu.unc.elmirador.comercial.models.vo.Dinero;
import pe.edu.unc.elmirador.comercial.models.entity.OrdenDeServicio;
import pe.edu.unc.elmirador.comercial.models.entity.PeticionIdempotente;
import pe.edu.unc.elmirador.comercial.models.vo.EstadoDeOrden;
import pe.edu.unc.elmirador.comercial.repositories.ClienteRepository;
import pe.edu.unc.elmirador.comercial.repositories.ContratoMarcoRepository;
import pe.edu.unc.elmirador.comercial.repositories.OrdenDeServicioRepository;
import pe.edu.unc.elmirador.comercial.repositories.PeticionIdempotenteRepository;

@Service
public class ComercialInternalService {

    private final OrdenDeServicioRepository ordenRepository;
    private final ClienteRepository clienteRepository;
    private final ContratoMarcoRepository contratoRepository;
    private final PeticionIdempotenteRepository idempotencia;
    private final Clock reloj;

    public ComercialInternalService(
            OrdenDeServicioRepository ordenRepository,
            ClienteRepository clienteRepository,
            ContratoMarcoRepository contratoRepository,
            PeticionIdempotenteRepository idempotencia,
            Clock reloj) {
        this.ordenRepository = ordenRepository;
        this.clienteRepository = clienteRepository;
        this.contratoRepository = contratoRepository;
        this.idempotencia = idempotencia;
        this.reloj = reloj;
    }

    /**
     * Contrato 1. El {@code 409} sale de {@code comoOrdenConfirmada()}: que una orden en BORRADOR o
     * CANCELADA no sea consultable para programar es una regla del agregado, no de esta capa.
     */
    @Transactional(readOnly = true)
    public OrdenConfirmadaResponse consultarOrdenConfirmada(String ordenId) {
        OrdenDeServicio orden = ordenRepository.findById(ordenId)
                .orElseThrow(() -> new RecursoNoEncontradoException("OrdenDeServicio", ordenId))
                .comoOrdenConfirmada();

        // Sin contrato marco no hay clausula que prohiba consolidar. La decision la nombra el objeto
        // de valor; aqui no se escribe un `true` suelto.
        ClausulaDeConsolidacion clausula = orden.contratoId() == null
                ? ClausulaDeConsolidacion.sinContratoMarco()
                : contratoRepository.findById(orden.contratoId())
                        .map(ContratoMarco::clausulaDeConsolidacion)
                        .orElseGet(ClausulaDeConsolidacion::sinContratoMarco);

        return new OrdenConfirmadaResponse(
                orden.id(),
                orden.clienteId(),
                orden.estado().name(),
                new OrdenConfirmadaResponse.CargaResponse(
                        orden.carga().pesoKg(),
                        orden.carga().volumenM3(),
                        orden.carga().embalaje(),
                        orden.carga().naturaleza()),
                new OrdenConfirmadaResponse.RutaResponse(
                        orden.ruta().origen(),
                        orden.ruta().destino(),
                        orden.ruta().corredor(),
                        orden.rutaDistanciaKm()),
                orden.ventana() == null ? null : new OrdenConfirmadaResponse.VentanaResponse(
                        orden.ventana().inicio(),
                        orden.ventana().fin()),
                clausula.permitida(),
                clausula.restricciones(),
                orden.tipoUnidadRequerido() == null ? null : orden.tipoUnidadRequerido().name());
    }

    @Transactional(readOnly = true)
    public SnapshotFacturableResponse consultarSnapshotFacturable(String ordenId) {
        OrdenDeServicio orden = ordenRepository.findById(ordenId)
                .orElseThrow(() -> new RecursoNoEncontradoException("OrdenDeServicio", ordenId));
        
        Cliente cliente = clienteRepository.findById(orden.clienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", orden.clienteId()));

        List<SnapshotFacturableResponse.RecargoResponse> recargos = orden.tarifa().recargos().stream()
                .map(r -> new SnapshotFacturableResponse.RecargoResponse(r.tipo().name(), r.porcentaje()))
                .toList();

        SnapshotFacturableResponse.DescuentoResponse descuento = null;
        if (orden.tarifa().descuento() != null) {
            descuento = new SnapshotFacturableResponse.DescuentoResponse(
                    orden.tarifa().descuento().porcentaje(),
                    "DESCUENTO"
            );
        }

        SnapshotFacturableResponse.TarifaResponse tarifaResponse = new SnapshotFacturableResponse.TarifaResponse(
                new SnapshotFacturableResponse.DineroResponse(
                        orden.tarifa().base().monto().toString(),
                        orden.tarifa().base().codigoMoneda()
                ),
                recargos,
                descuento,
                new SnapshotFacturableResponse.DineroResponse(
                        orden.tarifa().total().monto().toString(),
                        orden.tarifa().total().codigoMoneda()
                )
            );

        return new SnapshotFacturableResponse(
                orden.id(),
                cliente.id(),
                cliente.ruc().valor(),
                cliente.razonSocial().valor(),
                tarifaResponse,
                new SnapshotFacturableResponse.CondicionDePagoResponse(
                        orden.condicionDePago().modalidad().name(),
                        orden.condicionDePago().plazoEnDias()
                ),
                OffsetDateTime.now(reloj)
        );
    }

    /**
     * Contrato 7 · diferencia de carga. Idempotente por {@code Idempotency-Key} (regla 6).
     *
     * <p>La primera version de este slice guardaba la clave y descartaba el dato, de modo que la
     * proteccion contra duplicados protegia un efecto que no existia.
     */
    @Transactional
    public ResultadoIdempotente<DiferenciaRegistradaResponse> reportarDiferencia(
            String ordenId, String clave, DiferenciaDeCargaRequest peticion) {

        Optional<PeticionIdempotente> yaVista = idempotencia.findById(clave);
        if (yaVista.isPresent()) {
            return new ResultadoIdempotente<>(
                    new DiferenciaRegistradaResponse(ordenId, peticion.viajeId(), peticion.decision().name()), true);
        }

        OrdenDeServicio orden = ordenRepository.findById(ordenId)
                .orElseThrow(() -> new RecursoNoEncontradoException("OrdenDeServicio", ordenId));

        orden.registrarDiferenciaDeCarga(
                new Carga(
                        peticion.real().pesoKg(),
                        peticion.real().volumenM3(),
                        orden.carga().tipo(),
                        peticion.real().embalaje(),
                        orden.carga().naturaleza()),
                peticion.decision(),
                peticion.importeDelReajuste() == null ? null : new Dinero(
                        peticion.importeDelReajuste().monto(),
                        peticion.importeDelReajuste().moneda()));

        ordenRepository.save(orden);
        idempotencia.save(new PeticionIdempotente(clave, ordenId, OffsetDateTime.now(reloj)));

        return new ResultadoIdempotente<>(
                new DiferenciaRegistradaResponse(ordenId, peticion.viajeId(), peticion.decision().name()), false);
    }

    /** Contrato 7 · espera. El excedente lo calcula Ejecucion; aqui se guarda, no se recalcula. */
    @Transactional
    public ResultadoIdempotente<EsperaRegistradaResponse> reportarEspera(
            String ordenId, String clave, EsperaRequest peticion) {

        Optional<PeticionIdempotente> yaVista = idempotencia.findById(clave);
        if (yaVista.isPresent()) {
            return new ResultadoIdempotente<>(
                    new EsperaRegistradaResponse(ordenId, peticion.viajeId(), peticion.punto()), true);
        }

        OrdenDeServicio orden = ordenRepository.findById(ordenId)
                .orElseThrow(() -> new RecursoNoEncontradoException("OrdenDeServicio", ordenId));

        orden.registrarEspera(new EsperaRegistrada(
                UUID.randomUUID().toString(),
                peticion.viajeId(),
                peticion.punto(),
                peticion.tiempoLibreHoras(),
                peticion.tiempoRealHoras(),
                peticion.excedenteHoras()));

        ordenRepository.save(orden);
        idempotencia.save(new PeticionIdempotente(clave, ordenId, OffsetDateTime.now(reloj)));

        return new ResultadoIdempotente<>(
                new EsperaRegistradaResponse(ordenId, peticion.viajeId(), peticion.punto()), false);
    }
}
