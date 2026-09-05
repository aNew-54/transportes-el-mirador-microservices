package pe.edu.unc.elmirador.cobranza.controllers;

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

import pe.edu.unc.elmirador.cobranza.exceptions.AplicacionExcedeElPagoException;
import pe.edu.unc.elmirador.cobranza.exceptions.ConflictoDeRecursoException;
import pe.edu.unc.elmirador.cobranza.exceptions.DominioCobranzaException;
import pe.edu.unc.elmirador.cobranza.exceptions.ImportesInconsistentesException;
import pe.edu.unc.elmirador.cobranza.exceptions.MonedaIncompatibleException;
import pe.edu.unc.elmirador.cobranza.exceptions.PagoDeOtroClienteException;
import pe.edu.unc.elmirador.cobranza.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.cobranza.exceptions.RehabilitacionInvalidaException;
import pe.edu.unc.elmirador.cobranza.exceptions.SaldoInsuficienteException;

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

    @ExceptionHandler(RehabilitacionInvalidaException.class)
    public ProblemDetail rehabilitacionInvalida(RehabilitacionInvalidaException ex) {
        return problema(HttpStatus.CONFLICT, "rehabilitacion-invalida", ex.getMessage());
    }

    @ExceptionHandler(SaldoInsuficienteException.class)
    public ProblemDetail saldoInsuficiente(SaldoInsuficienteException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "saldo-insuficiente", ex.getMessage());
    }

    @ExceptionHandler(AplicacionExcedeElPagoException.class)
    public ProblemDetail aplicacionExcedeElPago(AplicacionExcedeElPagoException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "aplicacion-excede-el-pago", ex.getMessage());
    }

    @ExceptionHandler(PagoDeOtroClienteException.class)
    public ProblemDetail pagoDeOtroCliente(PagoDeOtroClienteException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "pago-de-otro-cliente", ex.getMessage());
    }

    @ExceptionHandler(ImportesInconsistentesException.class)
    public ProblemDetail importesInconsistentes(ImportesInconsistentesException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "importes-inconsistentes", ex.getMessage());
    }

    @ExceptionHandler(MonedaIncompatibleException.class)
    public ProblemDetail monedaIncompatible(MonedaIncompatibleException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "moneda-incompatible", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail argumentoInvalido(IllegalArgumentException ex) {
        return problema(HttpStatus.BAD_REQUEST, "argumento-invalido", ex.getMessage());
    }

    @ExceptionHandler(DominioCobranzaException.class)
    public ProblemDetail invarianteViolada(DominioCobranzaException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "invariante-violada", ex.getMessage());
    }

    private ProblemDetail problema(HttpStatus estado, String slug, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setType(URI.create(BASE_TIPO + slug));
        problema.setTitle(estado.getReasonPhrase());
        return problema;
    }
}
