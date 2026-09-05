package pe.edu.unc.elmirador.conductores.controllers;

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
import pe.edu.unc.elmirador.conductores.dto.internal.request.ReportarHorasRequest;
import pe.edu.unc.elmirador.conductores.dto.internal.request.ReportarIncidenciaRequest;
import pe.edu.unc.elmirador.conductores.dto.internal.response.ElegibilidadResponse;
import pe.edu.unc.elmirador.conductores.dto.internal.response.HorasRegistradasResponse;
import pe.edu.unc.elmirador.conductores.dto.internal.response.IncidenciaRegistradaResponse;
import pe.edu.unc.elmirador.conductores.dto.response.ResultadoIdempotente;
import pe.edu.unc.elmirador.conductores.models.vo.TipoDeUnidad;
import pe.edu.unc.elmirador.conductores.services.ConductorInternalService;

/**
 * Contratos 3 y 6 de {@code docs/api/contracts.md}. Regla 4: la integracion vive en
 * {@code /internal/v1} y no se expone a clientes externos.
 *
 * <p>Aparte del controlador publico a proposito: son dos audiencias y dos contratos, y un cambio
 * pedido por Programacion no debe poder mover la API publica.
 */
@RestController
@RequestMapping("/internal/v1/conductores")
public class ConductorInternalController {

    private final ConductorInternalService servicio;

    public ConductorInternalController(ConductorInternalService servicio) {
        this.servicio = servicio;
    }

    /** Contrato 3. {@code elegible: false} es un {@code 200} valido, nunca un error. */
    @GetMapping("/{conductorId}/elegibilidad")
    public ElegibilidadResponse elegibilidad(
            @PathVariable String conductorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta,
            @RequestParam TipoDeUnidad tipoUnidad,
            @RequestParam(required = false) String clienteId) {
        return servicio.elegibilidad(conductorId, desde, hasta, tipoUnidad, clienteId);
    }

    /**
     * Contrato 6 · horas. La cabecera {@code Idempotency-Key} es obligatoria: sin ella no se puede
     * distinguir un reintento de red de un segundo reporte, y las horas se sumarian dos veces.
     */
    @PostMapping("/{conductorId}/horas-conduccion")
    public ResponseEntity<HorasRegistradasResponse> reportarHoras(
            @PathVariable String conductorId,
            @RequestHeader("Idempotency-Key") String clave,
            @Valid @RequestBody ReportarHorasRequest peticion) {
        ResultadoIdempotente<HorasRegistradasResponse> resultado =
                servicio.reportarHoras(conductorId, clave, peticion);
        return ResponseEntity.status(HttpStatus.OK).body(resultado.cuerpo());
    }

    @PostMapping("/{conductorId}/incidencias")
    public ResponseEntity<IncidenciaRegistradaResponse> reportarIncidencia(
            @PathVariable String conductorId,
            @RequestHeader("Idempotency-Key") String clave,
            @Valid @RequestBody ReportarIncidenciaRequest peticion) {
        ResultadoIdempotente<IncidenciaRegistradaResponse> resultado =
                servicio.reportarIncidencia(conductorId, clave, peticion);
        return ResponseEntity.status(HttpStatus.OK).body(resultado.cuerpo());
    }
}
