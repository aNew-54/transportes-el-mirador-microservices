package pe.edu.unc.elmirador.unidades.controllers;

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

import pe.edu.unc.elmirador.unidades.exceptions.ConflictoDeRecursoException;
import pe.edu.unc.elmirador.unidades.exceptions.DominioUnidadesException;
import pe.edu.unc.elmirador.unidades.exceptions.OrdenCerradaException;
import pe.edu.unc.elmirador.unidades.exceptions.PlacaInvalidaException;
import pe.edu.unc.elmirador.unidades.exceptions.ReactivacionInvalidaException;
import pe.edu.unc.elmirador.unidades.exceptions.RecursoNoEncontradoException;

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

    @ExceptionHandler(OrdenCerradaException.class)
    public ProblemDetail ordenCerrada(OrdenCerradaException ex) {
        return problema(HttpStatus.CONFLICT, "orden-cerrada", ex.getMessage());
    }

    @ExceptionHandler(ReactivacionInvalidaException.class)
    public ProblemDetail reactivacionInvalida(ReactivacionInvalidaException ex) {
        return problema(HttpStatus.CONFLICT, "reactivacion-invalida", ex.getMessage());
    }

    @ExceptionHandler(pe.edu.unc.elmirador.unidades.exceptions.KilometrajeRetrocedeException.class)
    public ProblemDetail kilometrajeRetrocede(pe.edu.unc.elmirador.unidades.exceptions.KilometrajeRetrocedeException ex) {
        return problema(HttpStatus.CONFLICT, "kilometraje-retrocede", ex.getMessage());
    }

    @ExceptionHandler(PlacaInvalidaException.class)
    public ProblemDetail placaInvalida(PlacaInvalidaException ex) {
        return problema(HttpStatus.BAD_REQUEST, "placa-invalida", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail argumentoInvalido(IllegalArgumentException ex) {
        return problema(HttpStatus.BAD_REQUEST, "argumento-invalido", ex.getMessage());
    }

    @ExceptionHandler(DominioUnidadesException.class)
    public ProblemDetail invarianteViolada(DominioUnidadesException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "invariante-violada", ex.getMessage());
    }

    private ProblemDetail problema(HttpStatus estado, String slug, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setType(URI.create(BASE_TIPO + slug));
        problema.setTitle(estado.getReasonPhrase());
        return problema;
    }
}
