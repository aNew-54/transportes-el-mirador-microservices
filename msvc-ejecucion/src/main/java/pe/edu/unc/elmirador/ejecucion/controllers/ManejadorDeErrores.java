package pe.edu.unc.elmirador.ejecucion.controllers;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import pe.edu.unc.elmirador.ejecucion.exceptions.CheckListNoAprobadoException;
import pe.edu.unc.elmirador.ejecucion.exceptions.ConflictoDeRecursoException;
import pe.edu.unc.elmirador.ejecucion.exceptions.ConformidadesPendientesException;
import pe.edu.unc.elmirador.ejecucion.exceptions.DominioEjecucionException;
import pe.edu.unc.elmirador.ejecucion.exceptions.EjecucionEntregadaException;
import pe.edu.unc.elmirador.ejecucion.exceptions.EvidenciaRequeridaException;
import pe.edu.unc.elmirador.ejecucion.exceptions.GastoSinComprobanteException;
import pe.edu.unc.elmirador.ejecucion.exceptions.LiquidacionAprobadaException;
import pe.edu.unc.elmirador.ejecucion.exceptions.LiquidacionPendienteException;
import pe.edu.unc.elmirador.ejecucion.exceptions.MonedaIncompatibleException;
import pe.edu.unc.elmirador.ejecucion.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.ejecucion.exceptions.TransicionDeEjecucionInvalidaException;
import pe.edu.unc.elmirador.ejecucion.exceptions.ProgramacionIntegrationException;
import pe.edu.unc.elmirador.ejecucion.exceptions.UnidadesIntegrationException;
import pe.edu.unc.elmirador.ejecucion.exceptions.ConductoresIntegrationException;
import pe.edu.unc.elmirador.ejecucion.exceptions.ComercialIntegrationException;
import pe.edu.unc.elmirador.ejecucion.exceptions.FacturacionIntegrationException;

/**
 * El unico punto del modulo que conoce codigos HTTP.
 */
@RestControllerAdvice
public class ManejadorDeErrores extends ResponseEntityExceptionHandler {

    private static final String BASE_TIPO = "https://elmirador.unc.edu.pe/problems/";

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders cabeceras,
            HttpStatusCode estado,
            WebRequest peticion) {

        ProblemDetail problema = problema(
                HttpStatus.BAD_REQUEST,
                "validacion",
                "La peticion no supera la validacion de formato");

        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.put(error.getField(), error.getDefaultMessage());
        }
        problema.setProperty("errores", errores);

        HttpHeaders cabecerasProblema = new HttpHeaders();
        cabecerasProblema.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return new ResponseEntity<>(problema, cabecerasProblema, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail noEncontrado(RecursoNoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "recurso-no-encontrado", ex.getMessage());
    }

    @ExceptionHandler(ConflictoDeRecursoException.class)
    public ProblemDetail conflicto(ConflictoDeRecursoException ex) {
        return problema(HttpStatus.CONFLICT, "conflicto-de-recurso", ex.getMessage());
    }

    @ExceptionHandler({
            TransicionDeEjecucionInvalidaException.class,
            EjecucionEntregadaException.class,
            CheckListNoAprobadoException.class,
            ConformidadesPendientesException.class,
            LiquidacionAprobadaException.class,
            LiquidacionPendienteException.class
    })
    public ProblemDetail conflictoDeDominio(DominioEjecucionException ex) {
        // Excepciones de dominio que se traducen a 409
        String slug = "conflicto-de-dominio";
        if (ex instanceof TransicionDeEjecucionInvalidaException) slug = "transicion-invalida";
        else if (ex instanceof EjecucionEntregadaException) slug = "ejecucion-entregada";
        else if (ex instanceof CheckListNoAprobadoException) slug = "checklist-no-aprobado";
        else if (ex instanceof ConformidadesPendientesException) slug = "conformidades-pendientes";
        else if (ex instanceof LiquidacionAprobadaException) slug = "liquidacion-aprobada";
        else if (ex instanceof LiquidacionPendienteException) slug = "liquidacion-pendiente";

        return problema(HttpStatus.CONFLICT, slug, ex.getMessage());
    }

    @ExceptionHandler({
            GastoSinComprobanteException.class,
            EvidenciaRequeridaException.class,
            MonedaIncompatibleException.class
    })
    public ProblemDetail invalido(DominioEjecucionException ex) {
        // Excepciones especificas que se traducen a 422
        String slug = "invariante-violada";
        if (ex instanceof GastoSinComprobanteException) slug = "gasto-sin-comprobante";
        else if (ex instanceof EvidenciaRequeridaException) slug = "evidencia-requerida";
        else if (ex instanceof MonedaIncompatibleException) slug = "moneda-incompatible";
        
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, slug, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail argumentoInvalido(IllegalArgumentException ex) {
        return problema(HttpStatus.BAD_REQUEST, "argumento-invalido", ex.getMessage());
    }

    @ExceptionHandler(DominioEjecucionException.class)
    public ProblemDetail invarianteViolada(DominioEjecucionException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "invariante-violada", ex.getMessage());
    }

    @ExceptionHandler(ProgramacionIntegrationException.class)
    public ProblemDetail falloProgramacion(ProgramacionIntegrationException ex) {
        return problema(HttpStatus.SERVICE_UNAVAILABLE, "programacion-integration-error", ex.getMessage());
    }

    @ExceptionHandler(UnidadesIntegrationException.class)
    public ProblemDetail falloUnidades(UnidadesIntegrationException ex) {
        return problema(HttpStatus.SERVICE_UNAVAILABLE, "unidades-integration-error", ex.getMessage());
    }

    @ExceptionHandler(ConductoresIntegrationException.class)
    public ProblemDetail falloConductores(ConductoresIntegrationException ex) {
        return problema(HttpStatus.SERVICE_UNAVAILABLE, "conductores-integration-error", ex.getMessage());
    }

    @ExceptionHandler(ComercialIntegrationException.class)
    public ProblemDetail falloComercial(ComercialIntegrationException ex) {
        return problema(HttpStatus.SERVICE_UNAVAILABLE, "comercial-integration-error", ex.getMessage());
    }

    @ExceptionHandler(FacturacionIntegrationException.class)
    public ProblemDetail falloFacturacion(FacturacionIntegrationException ex) {
        return problema(HttpStatus.SERVICE_UNAVAILABLE, "facturacion-integration-error", ex.getMessage());
    }

    private ProblemDetail problema(HttpStatus estado, String slug, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setType(URI.create(BASE_TIPO + slug));
        problema.setTitle(estado.getReasonPhrase());
        return problema;
    }
}
