package pe.edu.unc.elmirador.unidades.controllers;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.edu.unc.elmirador.unidades.dto.internal.request.ReportarFallaRequest;
import pe.edu.unc.elmirador.unidades.dto.internal.request.ReportarKilometrajeRequest;
import pe.edu.unc.elmirador.unidades.dto.internal.response.ElegibilidadUnidadResponse;
import pe.edu.unc.elmirador.unidades.dto.internal.response.FallaRegistradaResponse;
import pe.edu.unc.elmirador.unidades.dto.internal.response.KilometrajeRegistradoResponse;
import pe.edu.unc.elmirador.unidades.dto.response.ResultadoIdempotente;
import pe.edu.unc.elmirador.unidades.models.vo.TipoDeCarga;
import pe.edu.unc.elmirador.unidades.services.UnidadInternalService;

@RestController
@RequestMapping("/internal/v1/unidades")
public class UnidadInternalController {

    private final UnidadInternalService servicio;

    public UnidadInternalController(UnidadInternalService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/{unidadId}/elegibilidad")
    public ElegibilidadUnidadResponse elegibilidad(
            @PathVariable String unidadId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta,
            @RequestParam Integer pesoKg,
            @RequestParam BigDecimal volumenM3,
            @RequestParam(required = false) TipoDeCarga tipoCargaRequerido) {
        return servicio.elegibilidad(unidadId, desde, hasta, pesoKg, volumenM3, tipoCargaRequerido);
    }

    @PostMapping("/{unidadId}/kilometraje")
    public ResponseEntity<KilometrajeRegistradoResponse> reportarKilometraje(
            @PathVariable String unidadId,
            @RequestHeader("Idempotency-Key") String clave,
            @Valid @RequestBody ReportarKilometrajeRequest peticion) {
        ResultadoIdempotente<KilometrajeRegistradoResponse> resultado =
                servicio.reportarKilometraje(unidadId, clave, peticion);
        return ResponseEntity.status(HttpStatus.OK).body(resultado.cuerpo());
    }

    @PostMapping("/{unidadId}/fallas")
    public ResponseEntity<FallaRegistradaResponse> reportarFalla(
            @PathVariable String unidadId,
            @RequestHeader("Idempotency-Key") String clave,
            @Valid @RequestBody ReportarFallaRequest peticion) {
        ResultadoIdempotente<FallaRegistradaResponse> resultado =
                servicio.reportarFalla(unidadId, clave, peticion);
        return ResponseEntity.status(HttpStatus.OK).body(resultado.cuerpo());
    }
}
