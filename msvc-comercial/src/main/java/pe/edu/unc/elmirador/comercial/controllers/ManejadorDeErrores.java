package pe.edu.unc.elmirador.comercial.controllers;

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

import pe.edu.unc.elmirador.comercial.exceptions.ConflictoDeRecursoException;
import pe.edu.unc.elmirador.comercial.exceptions.CotizacionVencidaException;
import pe.edu.unc.elmirador.comercial.exceptions.DominioComercialException;
import pe.edu.unc.elmirador.comercial.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.comercial.exceptions.RucInvalidoException;
import pe.edu.unc.elmirador.comercial.exceptions.TarifarioVigenteDuplicadoException;
import pe.edu.unc.elmirador.comercial.exceptions.TransicionDeOrdenInvalidaException;
import pe.edu.unc.elmirador.comercial.exceptions.ReajusteRequeridoException;

/**
 * El único punto del módulo que conoce códigos HTTP.
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

    @ExceptionHandler(RucInvalidoException.class)
    public ProblemDetail rucInvalido(RucInvalidoException ex) {
        return problema(HttpStatus.BAD_REQUEST, "ruc-invalido", ex.getMessage());
    }

    @ExceptionHandler(CotizacionVencidaException.class)
    public ProblemDetail cotizacionVencida(CotizacionVencidaException ex) {
        return problema(HttpStatus.CONFLICT, "cotizacion-vencida", ex.getMessage());
    }

    @ExceptionHandler(TransicionDeOrdenInvalidaException.class)
    public ProblemDetail transicionDeOrdenInvalida(TransicionDeOrdenInvalidaException ex) {
        return problema(HttpStatus.CONFLICT, "transicion-orden-invalida", ex.getMessage());
    }

    @ExceptionHandler(TarifarioVigenteDuplicadoException.class)
    public ProblemDetail tarifarioVigenteDuplicado(TarifarioVigenteDuplicadoException ex) {
        return problema(HttpStatus.CONFLICT, "tarifario-vigente-duplicado", ex.getMessage());
    }
    
    @ExceptionHandler(ReajusteRequeridoException.class)
    public ProblemDetail reajusteRequerido(ReajusteRequeridoException ex) {
        return problema(HttpStatus.CONFLICT, "reajuste-requerido", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail argumentoInvalido(IllegalArgumentException ex) {
        return problema(HttpStatus.BAD_REQUEST, "argumento-invalido", ex.getMessage());
    }

    @ExceptionHandler(DominioComercialException.class)
    public ProblemDetail invarianteViolada(DominioComercialException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "invariante-violada", ex.getMessage());
    }

    private ProblemDetail problema(HttpStatus estado, String slug, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setType(URI.create(BASE_TIPO + slug));
        problema.setTitle(estado.getReasonPhrase());
        return problema;
    }
}
