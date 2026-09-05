package pe.edu.unc.elmirador.comercial.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.edu.unc.elmirador.comercial.dto.internal.request.DiferenciaDeCargaRequest;
import pe.edu.unc.elmirador.comercial.dto.internal.request.EsperaRequest;
import pe.edu.unc.elmirador.comercial.dto.internal.response.DiferenciaRegistradaResponse;
import pe.edu.unc.elmirador.comercial.dto.internal.response.EsperaRegistradaResponse;
import pe.edu.unc.elmirador.comercial.dto.internal.response.OrdenConfirmadaResponse;
import pe.edu.unc.elmirador.comercial.dto.internal.response.SnapshotFacturableResponse;
import pe.edu.unc.elmirador.comercial.dto.response.ResultadoIdempotente;
import pe.edu.unc.elmirador.comercial.services.ComercialInternalService;

@RestController
@RequestMapping("/internal/v1/ordenes")
public class ComercialInternalController {

    private final ComercialInternalService servicio;

    public ComercialInternalController(ComercialInternalService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/{ordenId}")
    public OrdenConfirmadaResponse consultarOrdenConfirmada(@PathVariable String ordenId) {
        return servicio.consultarOrdenConfirmada(ordenId);
    }

    @GetMapping("/{ordenId}/snapshot-facturable")
    public SnapshotFacturableResponse consultarSnapshotFacturable(@PathVariable String ordenId) {
        return servicio.consultarSnapshotFacturable(ordenId);
    }

    @PostMapping("/{ordenId}/diferencias-de-carga")
    public ResponseEntity<DiferenciaRegistradaResponse> reportarDiferencia(
            @PathVariable String ordenId,
            @RequestHeader("Idempotency-Key") String clave,
            @Valid @RequestBody DiferenciaDeCargaRequest peticion) {
        ResultadoIdempotente<DiferenciaRegistradaResponse> resultado = 
                servicio.reportarDiferencia(ordenId, clave, peticion);
        return ResponseEntity.status(HttpStatus.OK).body(resultado.cuerpo());
    }

    @PostMapping("/{ordenId}/esperas")
    public ResponseEntity<EsperaRegistradaResponse> reportarEspera(
            @PathVariable String ordenId,
            @RequestHeader("Idempotency-Key") String clave,
            @Valid @RequestBody EsperaRequest peticion) {
        ResultadoIdempotente<EsperaRegistradaResponse> resultado =
                servicio.reportarEspera(ordenId, clave, peticion);
        return ResponseEntity.status(HttpStatus.OK).body(resultado.cuerpo());
    }
}
