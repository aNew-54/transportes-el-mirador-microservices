package pe.edu.unc.elmirador.cobranza.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.edu.unc.elmirador.cobranza.dto.request.RegistrarCuentaPorCobrarRequest;
import pe.edu.unc.elmirador.cobranza.dto.response.CuentaPorCobrarResponse;
import pe.edu.unc.elmirador.cobranza.dto.response.EstadoCrediticioResponse;
import pe.edu.unc.elmirador.cobranza.services.CuentaCorrienteService;
import java.net.URI;

@RestController
@RequestMapping("/internal/v1")
public class CuentaCorrienteInternalController {

    private final CuentaCorrienteService servicio;

    public CuentaCorrienteInternalController(CuentaCorrienteService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/cuentas-por-cobrar")
    public ResponseEntity<CuentaPorCobrarResponse> registrar(@Valid @RequestBody RegistrarCuentaPorCobrarRequest peticion) {
        CuentaPorCobrarResponse respuesta = servicio.registrarCuentaPorCobrar(peticion);
        return ResponseEntity.created(URI.create("/internal/v1/cuentas-por-cobrar/" + respuesta.id())).body(respuesta);
    }

    @GetMapping("/clientes/{clienteId}/estado-crediticio")
    public EstadoCrediticioResponse estadoCrediticio(@PathVariable String clienteId) {
        return servicio.estadoCrediticio(clienteId);
    }
}
