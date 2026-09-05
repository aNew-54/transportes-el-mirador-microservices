package pe.edu.unc.elmirador.programacion.controllers;

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
import pe.edu.unc.elmirador.programacion.exceptions.AsignacionIncompletaException;
import pe.edu.unc.elmirador.programacion.exceptions.CapacidadExcedidaException;
import pe.edu.unc.elmirador.programacion.exceptions.CargaIncompatibleException;
import pe.edu.unc.elmirador.programacion.exceptions.ConflictoDeRecursoException;
import pe.edu.unc.elmirador.programacion.exceptions.ConsolidacionProhibidaException;
import pe.edu.unc.elmirador.programacion.exceptions.CorredorIncompatibleException;
import pe.edu.unc.elmirador.programacion.exceptions.DominioProgramacionException;
import pe.edu.unc.elmirador.programacion.exceptions.RecursoNoElegibleException;
import pe.edu.unc.elmirador.programacion.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.programacion.exceptions.ReservaSolapadaException;
import pe.edu.unc.elmirador.programacion.exceptions.TransicionDeViajeInvalidaException;
import pe.edu.unc.elmirador.programacion.exceptions.ViajeDespachadoException;

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

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail argumentoInvalido(IllegalArgumentException ex) {
        return problema(HttpStatus.BAD_REQUEST, "argumento-invalido", ex.getMessage());
    }

    @ExceptionHandler(TransicionDeViajeInvalidaException.class)
    public ProblemDetail transicionInvalida(TransicionDeViajeInvalidaException ex) {
        return problema(HttpStatus.CONFLICT, "transicion-invalida", ex.getMessage());
    }

    @ExceptionHandler(ViajeDespachadoException.class)
    public ProblemDetail viajeDespachado(ViajeDespachadoException ex) {
        return problema(HttpStatus.CONFLICT, "viaje-despachado", ex.getMessage());
    }

    @ExceptionHandler(AsignacionIncompletaException.class)
    public ProblemDetail asignacionIncompleta(AsignacionIncompletaException ex) {
        return problema(HttpStatus.CONFLICT, "asignacion-incompleta", ex.getMessage());
    }

    @ExceptionHandler(ReservaSolapadaException.class)
    public ProblemDetail reservaSolapada(ReservaSolapadaException ex) {
        return problema(HttpStatus.CONFLICT, "reserva-solapada", ex.getMessage());
    }

    @ExceptionHandler(RecursoNoElegibleException.class)
    public ProblemDetail recursoNoElegible(RecursoNoElegibleException ex) {
        return problema(HttpStatus.CONFLICT, "recurso-no-elegible", ex.getMessage());
    }

    @ExceptionHandler(CapacidadExcedidaException.class)
    public ProblemDetail capacidadExcedida(CapacidadExcedidaException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "capacidad-excedida", ex.getMessage());
    }

    @ExceptionHandler(CorredorIncompatibleException.class)
    public ProblemDetail corredorIncompatible(CorredorIncompatibleException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "corredor-incompatible", ex.getMessage());
    }

    @ExceptionHandler(ConsolidacionProhibidaException.class)
    public ProblemDetail consolidacionProhibida(ConsolidacionProhibidaException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "consolidacion-prohibida", ex.getMessage());
    }

    @ExceptionHandler(CargaIncompatibleException.class)
    public ProblemDetail cargaIncompatible(CargaIncompatibleException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "carga-incompatible", ex.getMessage());
    }

    @ExceptionHandler(DominioProgramacionException.class)
    public ProblemDetail invarianteViolada(DominioProgramacionException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "invariante-violada", ex.getMessage());
    }

    private ProblemDetail problema(HttpStatus estado, String slug, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setType(URI.create(BASE_TIPO + slug));
        problema.setTitle(estado.getReasonPhrase());
        return problema;
    }
}
