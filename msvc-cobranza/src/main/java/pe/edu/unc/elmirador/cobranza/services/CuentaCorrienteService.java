package pe.edu.unc.elmirador.cobranza.services;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unc.elmirador.cobranza.dto.request.RegistrarCuentaPorCobrarRequest;
import pe.edu.unc.elmirador.cobranza.dto.response.CarteraGestionResponse;
import pe.edu.unc.elmirador.cobranza.dto.response.CuentaCorrienteResponse;
import pe.edu.unc.elmirador.cobranza.dto.response.CuentaPorCobrarResponse;
import pe.edu.unc.elmirador.cobranza.dto.response.EstadoCrediticioResponse;
import pe.edu.unc.elmirador.cobranza.exceptions.ConflictoDeRecursoException;
import pe.edu.unc.elmirador.cobranza.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.cobranza.mappers.CuentaCorrienteMapper;
import pe.edu.unc.elmirador.cobranza.models.entity.CuentaCorrienteDelCliente;
import pe.edu.unc.elmirador.cobranza.models.entity.CuentaPorCobrar;
import pe.edu.unc.elmirador.cobranza.models.vo.Dinero;
import pe.edu.unc.elmirador.cobranza.models.vo.EstadoCrediticio;
import pe.edu.unc.elmirador.cobranza.models.vo.EstadoDeDocumento;
import pe.edu.unc.elmirador.cobranza.models.vo.TramoDeGestion;
import pe.edu.unc.elmirador.cobranza.repositories.CuentaCorrienteDelClienteRepository;

@Service
public class CuentaCorrienteService {

    private final CuentaCorrienteDelClienteRepository repositorio;
    private final Clock reloj;

    public CuentaCorrienteService(CuentaCorrienteDelClienteRepository repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public CuentaCorrienteResponse porClienteId(String clienteId) {
        LocalDate hoy = LocalDate.now(reloj);
        CuentaCorrienteDelCliente cuenta = buscarPorClienteId(clienteId);
        return CuentaCorrienteMapper.aRespuesta(cuenta, hoy);
    }

    @Transactional(readOnly = true)
    public List<CuentaPorCobrarResponse> listarCuentasPorCobrar(String clienteId, EstadoDeDocumento estado, Integer atrasoMinimo) {
        LocalDate hoy = LocalDate.now(reloj);
        List<CuentaCorrienteDelCliente> clientes = clienteId != null 
                ? repositorio.findByClienteId(clienteId).map(List::of).orElse(List.of())
                : repositorio.findAll();

        return clientes.stream()
                .flatMap(c -> c.cuentas().stream())
                .filter(c -> estado == null || c.estadoEn(hoy) == estado)
                .filter(c -> atrasoMinimo == null || c.diasDeAtraso(hoy).dias() >= atrasoMinimo)
                .map(c -> CuentaCorrienteMapper.aRespuesta(c, hoy))
                .toList();
    }

    @Transactional
    public CuentaPorCobrarResponse registrarCuentaPorCobrar(RegistrarCuentaPorCobrarRequest peticion) {
        CuentaCorrienteDelCliente cliente = repositorio.findByClienteId(peticion.clienteId())
                .orElseGet(() -> {
                    EstadoCrediticio estadoVigente = EstadoCrediticio.vigente(LocalDate.now(reloj));
                    return new CuentaCorrienteDelCliente(peticion.clienteId(), estadoVigente);
                });

        boolean existeFactura = cliente.cuentas().stream()
                .anyMatch(c -> c.facturaId().equals(peticion.facturaId()));
        
        if (existeFactura) {
            throw new ConflictoDeRecursoException("La factura " + peticion.facturaId() + " ya se encuentra registrada");
        }

        CuentaPorCobrar nuevaCuenta = new CuentaPorCobrar(
                UUID.randomUUID().toString(),
                peticion.clienteId(),
                peticion.facturaId(),
                peticion.documentoId(),
                new Dinero(peticion.totalMonto(), peticion.totalMoneda()),
                new Dinero(peticion.detraccionMonto(), peticion.detraccionMoneda()),
                new Dinero(peticion.montoNetoMonto(), peticion.montoNetoMoneda()),
                peticion.fechaDeVencimiento()
        );

        cliente.registrarCuenta(nuevaCuenta);
        repositorio.save(cliente);

        return CuentaCorrienteMapper.aRespuesta(nuevaCuenta, LocalDate.now(reloj));
    }

    @Transactional
    public CuentaPorCobrarResponse registrarDetraccion(String id) {
        CuentaCorrienteDelCliente cliente = repositorio.findAll().stream()
                .filter(c -> c.cuentas().stream().anyMatch(cuenta -> cuenta.id().equals(id)))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("cuenta por cobrar", id));

        CuentaPorCobrar cuentaPorCobrar = cliente.cuentas().stream()
                .filter(c -> c.id().equals(id))
                .findFirst()
                .orElseThrow();

        cuentaPorCobrar.registrarDepositoDeDetraccion();
        repositorio.save(cliente);

        return CuentaCorrienteMapper.aRespuesta(cuentaPorCobrar, LocalDate.now(reloj));
    }

    @Transactional
    public CuentaCorrienteResponse rehabilitar(String clienteId) {
        CuentaCorrienteDelCliente cliente = buscarPorClienteId(clienteId);
        cliente.rehabilitarCredito(LocalDate.now(reloj));
        return CuentaCorrienteMapper.aRespuesta(repositorio.save(cliente), LocalDate.now(reloj));
    }

    @Transactional(readOnly = true)
    public EstadoCrediticioResponse estadoCrediticio(String clienteId) {
        CuentaCorrienteDelCliente cliente = buscarPorClienteId(clienteId);
        return CuentaCorrienteMapper.aEstadoCrediticioRespuesta(cliente);
    }

    @Transactional(readOnly = true)
    public List<CarteraGestionResponse> carteraGestion() {
        LocalDate hoy = LocalDate.now(reloj);
        List<CuentaPorCobrar> todasLasCuentas = repositorio.findAll().stream()
                .flatMap(c -> c.cuentas().stream())
                .filter(c -> !c.estaCancelada())
                .toList();

        Map<TramoDeGestion, List<CuentaPorCobrar>> agrupadas = todasLasCuentas.stream()
                .collect(Collectors.groupingBy(c -> c.diasDeAtraso(hoy).tramoDeGestion()));

        return agrupadas.entrySet().stream()
                .map(entry -> new CarteraGestionResponse(
                        entry.getKey(),
                        entry.getValue().stream().map(c -> CuentaCorrienteMapper.aRespuesta(c, hoy)).toList()
                ))
                .toList();
    }
    
    @Transactional
    public CuentaCorrienteResponse evaluarCredito(String clienteId) {
        CuentaCorrienteDelCliente cliente = buscarPorClienteId(clienteId);
        cliente.evaluarCredito(LocalDate.now(reloj));
        return CuentaCorrienteMapper.aRespuesta(repositorio.save(cliente), LocalDate.now(reloj));
    }

    private CuentaCorrienteDelCliente buscarPorClienteId(String clienteId) {
        return repositorio.findByClienteId(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("cliente", clienteId));
    }
}
