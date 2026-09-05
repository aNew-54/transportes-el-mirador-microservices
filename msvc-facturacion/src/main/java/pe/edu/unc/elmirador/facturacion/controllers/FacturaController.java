package pe.edu.unc.elmirador.facturacion.controllers;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.edu.unc.elmirador.facturacion.dto.request.AbrirFacturaRequest;
import pe.edu.unc.elmirador.facturacion.dto.request.EmitirFacturaRequest;
import pe.edu.unc.elmirador.facturacion.dto.request.EmitirFalsoFleteRequest;
import pe.edu.unc.elmirador.facturacion.dto.request.RegistrarConformidadRequest;
import pe.edu.unc.elmirador.facturacion.dto.response.FacturaResponse;
import pe.edu.unc.elmirador.facturacion.models.vo.EstadoDeFactura;
import pe.edu.unc.elmirador.facturacion.services.FacturaService;

@RestController
public class FacturaController {

    private final FacturaService servicio;

    public FacturaController(FacturaService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/api/v1/facturas")
    public ResponseEntity<FacturaResponse> abrir(@Valid @RequestBody AbrirFacturaRequest peticion) {
        FacturaResponse creada = servicio.abrir(peticion);
        return ResponseEntity.created(URI.create("/api/v1/facturas/" + creada.id())).body(creada);
    }

    @PostMapping("/api/v1/facturas/{id}/emitir")
    public FacturaResponse emitir(
            @PathVariable String id,
            @Valid @RequestBody EmitirFacturaRequest peticion) {
        return servicio.emitir(id, peticion);
    }

    @PostMapping("/api/v1/facturas/{id}/anular")
    public FacturaResponse anular(@PathVariable String id) {
        return servicio.anular(id);
    }

    @GetMapping("/api/v1/facturas/{id}")
    public FacturaResponse porId(@PathVariable String id) {
        return servicio.porId(id);
    }

    @GetMapping("/api/v1/facturas")
    public List<FacturaResponse> listar(
            @RequestParam(required = false) EstadoDeFactura estado,
            @RequestParam(required = false) String clienteId,
            @RequestParam(required = false) LocalDate fecha) {
        return servicio.listar(estado, clienteId, fecha);
    }

    @PostMapping("/api/v1/facturas/falso-flete")
    public ResponseEntity<FacturaResponse> emitirFalsoFlete(
            @Valid @RequestBody EmitirFalsoFleteRequest peticion) {
        FacturaResponse creada = servicio.emitirFalsoFlete(peticion);
        return ResponseEntity.created(URI.create("/api/v1/facturas/" + creada.id())).body(creada);
    }

    @PostMapping("/internal/v1/conformidades")
    public ResponseEntity<Void> registrarConformidad(
            @Valid @RequestBody RegistrarConformidadRequest peticion) {
        servicio.registrarConformidad(peticion);
        return ResponseEntity.ok().build();
    }
}
