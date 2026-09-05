package pe.edu.unc.elmirador.facturacion.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.edu.unc.elmirador.facturacion.dto.internal.request.RegistrarConformidadRequest;
import pe.edu.unc.elmirador.facturacion.dto.internal.response.ConformidadRegistradaResponse;
import pe.edu.unc.elmirador.facturacion.dto.response.ResultadoIdempotente;
import pe.edu.unc.elmirador.facturacion.services.FacturacionInternalService;

/**
 * Contrato 8 de {@code docs/api/contracts.md}.
 */
@RestController
@RequestMapping("/internal/v1")
public class FacturacionInternalController {

    private final FacturacionInternalService servicio;

    public FacturacionInternalController(FacturacionInternalService servicio) {
        this.servicio = servicio;
    }

    /**
     * Contrato 8 · conformidades. La cabecera {@code Idempotency-Key} es obligatoria.
     */
    @PostMapping("/conformidades")
    public ResponseEntity<ConformidadRegistradaResponse> registrarConformidad(
            @RequestHeader("Idempotency-Key") String clave,
            @Valid @RequestBody RegistrarConformidadRequest peticion) {
        ResultadoIdempotente<ConformidadRegistradaResponse> resultado =
                servicio.registrarConformidad(clave, peticion);
        return ResponseEntity.status(HttpStatus.OK).body(resultado.cuerpo());
    }
}
