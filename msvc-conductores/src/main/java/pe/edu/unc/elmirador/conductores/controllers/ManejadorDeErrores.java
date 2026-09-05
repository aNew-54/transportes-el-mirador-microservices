package pe.edu.unc.elmirador.conductores.controllers;

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

import pe.edu.unc.elmirador.conductores.exceptions.ConflictoDeRecursoException;
import pe.edu.unc.elmirador.conductores.exceptions.DominioConductoresException;
import pe.edu.unc.elmirador.conductores.exceptions.NumeroDeLicenciaInvalidoException;
import pe.edu.unc.elmirador.conductores.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.conductores.exceptions.RehabilitacionInvalidaException;

/**
 * El unico punto del modulo que conoce codigos HTTP (regla 5: {@code application/problem+json}).
 *
 * <p>Spring elige el {@code @ExceptionHandler} mas especifico, asi que las excepciones que merecen
 * {@code 400} o {@code 409} se listan una a una y {@link DominioConductoresException} queda de
 * comodin en {@code 422}. Una excepcion de dominio nueva que nadie recuerde declarar cae ahi, que es
 * el codigo correcto por defecto. Nunca un {@code 500}.
 *
 * <p>{@code 409} frente a {@code 422}: {@code 409} es «ahora no» — el mismo cuerpo funcionaria con el
 * agregado en otro estado. {@code 422} es «asi no» — el cuerpo esta mal y seguira estandolo.
 */
@RestControllerAdvice
public class ManejadorDeErrores extends ResponseEntityExceptionHandler {

    private static final String BASE_TIPO = "https://elmirador.unc.edu.pe/problems/";

    /** El JSON no supera la validacion de forma. Devuelve el detalle campo a campo. */
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

    /** El agregado no existe. */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail noEncontrado(RecursoNoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "recurso-no-encontrado", ex.getMessage());
    }

    /** Ya existe otro conductor con ese numero de licencia. */
    @ExceptionHandler(ConflictoDeRecursoException.class)
    public ProblemDetail conflicto(ConflictoDeRecursoException ex) {
        return problema(HttpStatus.CONFLICT, "conflicto-de-recurso", ex.getMessage());
    }

    /**
     * CON-01 al rehabilitar: la operacion no cabe en el estado actual, pero cabria con la licencia
     * renovada. Es «ahora no», no «asi no».
     */
    @ExceptionHandler(RehabilitacionInvalidaException.class)
    public ProblemDetail rehabilitacionInvalida(RehabilitacionInvalidaException ex) {
        return problema(HttpStatus.CONFLICT, "rehabilitacion-invalida", ex.getMessage());
    }

    /** El objeto de valor rechaza el formato del dato: es un fallo de entrada, no de invariante. */
    @ExceptionHandler(NumeroDeLicenciaInvalidoException.class)
    public ProblemDetail licenciaInvalida(NumeroDeLicenciaInvalidoException ex) {
        return problema(HttpStatus.BAD_REQUEST, "numero-de-licencia-invalido", ex.getMessage());
    }

    /** Un argumento nulo o fuera de rango que la validacion de forma no llego a cubrir. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail argumentoInvalido(IllegalArgumentException ex) {
        return problema(HttpStatus.BAD_REQUEST, "argumento-invalido", ex.getMessage());
    }

    /** Comodin: los datos son validos pero rompen una invariante del diseno tactico. */
    @ExceptionHandler(DominioConductoresException.class)
    public ProblemDetail invarianteViolada(DominioConductoresException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "invariante-violada", ex.getMessage());
    }

    private ProblemDetail problema(HttpStatus estado, String slug, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setType(URI.create(BASE_TIPO + slug));
        problema.setTitle(estado.getReasonPhrase());
        return problema;
    }
}
