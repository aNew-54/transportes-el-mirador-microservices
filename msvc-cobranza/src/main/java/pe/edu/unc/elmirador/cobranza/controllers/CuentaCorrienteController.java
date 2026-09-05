package pe.edu.unc.elmirador.cobranza.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.unc.elmirador.cobranza.dto.response.CuentaCorrienteResponse;
import pe.edu.unc.elmirador.cobranza.dto.response.CuentaPorCobrarResponse;
import pe.edu.unc.elmirador.cobranza.models.vo.EstadoDeDocumento;
import pe.edu.unc.elmirador.cobranza.services.CuentaCorrienteService;

@RestController
@RequestMapping("/api/v1")
public class CuentaCorrienteController {

    private final CuentaCorrienteService servicio;

    public CuentaCorrienteController(CuentaCorrienteService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/cuentas-corrientes/{clienteId}")
    public CuentaCorrienteResponse porId(@PathVariable String clienteId) {
        return servicio.porClienteId(clienteId);
    }

    @GetMapping("/cuentas-por-cobrar")
    public List<CuentaPorCobrarResponse> listar(
            @RequestParam(required = false) String clienteId,
            @RequestParam(required = false) EstadoDeDocumento estado,
            @RequestParam(required = false) Integer atrasoMinimo) {
        return servicio.listarCuentasPorCobrar(clienteId, estado, atrasoMinimo);
    }

    @PostMapping("/cuentas-por-cobrar/{id}/detraccion")
    public CuentaPorCobrarResponse registrarDetraccion(@PathVariable String id) {
        return servicio.registrarDetraccion(id);
    }

    @PostMapping("/cuentas-corrientes/{clienteId}/rehabilitar")
    public CuentaCorrienteResponse rehabilitar(@PathVariable String clienteId) {
        return servicio.rehabilitar(clienteId);
    }
}
