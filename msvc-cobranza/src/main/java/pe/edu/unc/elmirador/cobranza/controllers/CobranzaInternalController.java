package pe.edu.unc.elmirador.cobranza.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.unc.elmirador.cobranza.dto.internal.request.CrearCuentaPorCobrarRequest;
import pe.edu.unc.elmirador.cobranza.dto.internal.response.CuentaPorCobrarCreadaResponse;
import pe.edu.unc.elmirador.cobranza.dto.internal.response.EstadoCrediticioResponse;
import pe.edu.unc.elmirador.cobranza.dto.response.ResultadoIdempotente;
import pe.edu.unc.elmirador.cobranza.services.CobranzaInternalService;

@RestController
@RequestMapping("/internal/v1")
public class CobranzaInternalController {

    private final CobranzaInternalService servicio;

    public CobranzaInternalController(CobranzaInternalService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/cuentas-por-cobrar")
    public ResponseEntity<CuentaPorCobrarCreadaResponse> crearCuentaPorCobrar(
            @RequestHeader("Idempotency-Key") String clave,
            @Valid @RequestBody CrearCuentaPorCobrarRequest peticion) {
        ResultadoIdempotente<CuentaPorCobrarCreadaResponse> resultado =
                servicio.crearCuentaPorCobrar(clave, peticion);
        
        HttpStatus status = resultado.repetida() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(resultado.cuerpo());
    }

    @GetMapping("/clientes/{clienteId}/estado-crediticio")
    public ResponseEntity<EstadoCrediticioResponse> estadoCrediticio(
            @PathVariable String clienteId) {
        EstadoCrediticioResponse response = servicio.estadoCrediticio(clienteId);
        return ResponseEntity.ok(response);
    }
}
