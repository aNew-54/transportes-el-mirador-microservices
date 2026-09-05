package pe.edu.unc.elmirador.cobranza.services;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unc.elmirador.cobranza.dto.response.CarteraGestionResponse;
import pe.edu.unc.elmirador.cobranza.dto.response.CuentaCorrienteResponse;
import pe.edu.unc.elmirador.cobranza.dto.response.CuentaPorCobrarResponse;
import pe.edu.unc.elmirador.cobranza.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.cobranza.mappers.CuentaCorrienteMapper;
import pe.edu.unc.elmirador.cobranza.models.entity.CuentaCorrienteDelCliente;
import pe.edu.unc.elmirador.cobranza.models.entity.CuentaPorCobrar;
import pe.edu.unc.elmirador.cobranza.models.vo.EstadoDeDocumento;
import pe.edu.unc.elmirador.cobranza.models.vo.TramoDeGestion;
import pe.edu.unc.elmirador.cobranza.repositories.CuentaCorrienteDelClienteRepository;

/**
 * Servicio de aplicacion del agregado {@code CuentaCorrienteDelCliente}.
 *
 * <p>Las cuentas por cobrar NO se dan de alta desde aqui: entran por el contrato 10, que Facturacion
 * empuja a {@code /internal/v1}, y eso es trabajo de {@code S4}. La superficie interna vive alli con
 * su validacion de FAC-04 en la frontera, no aqui.
 */
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
        return CuentaCorrienteMapper.aRespuesta(buscarPorClienteId(clienteId), LocalDate.now(reloj));
    }

    @Transactional(readOnly = true)
    public List<CuentaPorCobrarResponse> listarCuentasPorCobrar(
            String clienteId, EstadoDeDocumento estado, Integer atrasoMinimo) {

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
    public CuentaPorCobrarResponse registrarDetraccion(String id) {
        CuentaCorrienteDelCliente titular = repositorio.findByCuentasId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("cuenta por cobrar", id));
        CuentaPorCobrar cuenta = cuentaDe(titular, id);

        cuenta.registrarDepositoDeDetraccion();
        repositorio.save(titular);

        return CuentaCorrienteMapper.aRespuesta(cuenta, LocalDate.now(reloj));
    }

    @Transactional
    public CuentaCorrienteResponse rehabilitar(String clienteId) {
        LocalDate hoy = LocalDate.now(reloj);
        CuentaCorrienteDelCliente cliente = buscarPorClienteId(clienteId);
        cliente.rehabilitarCredito(hoy);
        return CuentaCorrienteMapper.aRespuesta(repositorio.save(cliente), hoy);
    }

    @Transactional(readOnly = true)
    public List<CarteraGestionResponse> carteraGestion() {
        LocalDate hoy = LocalDate.now(reloj);

        Map<TramoDeGestion, List<CuentaPorCobrar>> agrupadas = repositorio.findAll().stream()
                .flatMap(c -> c.cuentas().stream())
                .filter(c -> !c.estaCancelada())
                .collect(Collectors.groupingBy(c -> c.diasDeAtraso(hoy).tramoDeGestion()));

        return agrupadas.entrySet().stream()
                .map(entrada -> new CarteraGestionResponse(
                        entrada.getKey(),
                        entrada.getValue().stream()
                                .map(c -> CuentaCorrienteMapper.aRespuesta(c, hoy))
                                .toList()
                ))
                .toList();
    }

    private CuentaCorrienteDelCliente buscarPorClienteId(String clienteId) {
        return repositorio.findByClienteId(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("cliente", clienteId));
    }

    /** El titular ya vino del repositorio buscando por esta cuenta, asi que existe. */
    private static CuentaPorCobrar cuentaDe(CuentaCorrienteDelCliente titular, String cuentaId) {
        return titular.cuentas().stream()
                .filter(c -> c.id().equals(cuentaId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "El repositorio devolvio al titular de la cuenta " + cuentaId + " sin esa cuenta"));
    }
}
