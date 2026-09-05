package pe.edu.unc.elmirador.facturacion.controllers;

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
import pe.edu.unc.elmirador.facturacion.exceptions.*;

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

    @ExceptionHandler(NumeroDeComprobanteInvalidoException.class)
    public ProblemDetail comprobanteInvalido(NumeroDeComprobanteInvalidoException ex) {
        return problema(HttpStatus.BAD_REQUEST, "numero-de-comprobante-invalido", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail argumentoInvalido(IllegalArgumentException ex) {
        return problema(HttpStatus.BAD_REQUEST, "argumento-invalido", ex.getMessage());
    }

    @ExceptionHandler(FacturaInmutableException.class)
    public ProblemDetail facturaInmutable(FacturaInmutableException ex) {
        return problema(HttpStatus.CONFLICT, "factura-inmutable", ex.getMessage());
    }

    @ExceptionHandler(EmisionSinConformidadException.class)
    public ProblemDetail emisionSinConformidad(EmisionSinConformidadException ex) {
        return problema(HttpStatus.CONFLICT, "emision-sin-conformidad", ex.getMessage());
    }

    @ExceptionHandler(IncidenciaSinResolverException.class)
    public ProblemDetail incidenciaSinResolver(IncidenciaSinResolverException ex) {
        return problema(HttpStatus.CONFLICT, "incidencia-sin-resolver", ex.getMessage());
    }

    @ExceptionHandler(ImportesInconsistentesException.class)
    public ProblemDetail importesInconsistentes(ImportesInconsistentesException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "importes-inconsistentes", ex.getMessage());
    }

    @ExceptionHandler(MontoExcedeElSaldoException.class)
    public ProblemDetail montoExcedeSaldo(MontoExcedeElSaldoException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "monto-excede-el-saldo", ex.getMessage());
    }

    @ExceptionHandler(MonedaIncompatibleException.class)
    public ProblemDetail monedaIncompatible(MonedaIncompatibleException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "moneda-incompatible", ex.getMessage());
    }

    @ExceptionHandler(DominioFacturacionException.class)
    public ProblemDetail invariante(DominioFacturacionException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "invariante-violada", ex.getMessage());
    }

    @ExceptionHandler(pe.edu.unc.elmirador.facturacion.exceptions.ComercialIntegrationException.class)
    public ProblemDetail integracionComercial(pe.edu.unc.elmirador.facturacion.exceptions.ComercialIntegrationException ex) {
        return problema(HttpStatus.SERVICE_UNAVAILABLE, "falla-integracion-comercial", ex.getMessage());
    }

    @ExceptionHandler(pe.edu.unc.elmirador.facturacion.exceptions.CobranzaIntegrationException.class)
    public ProblemDetail integracionCobranza(pe.edu.unc.elmirador.facturacion.exceptions.CobranzaIntegrationException ex) {
        return problema(HttpStatus.SERVICE_UNAVAILABLE, "falla-integracion-cobranza", ex.getMessage());
    }

    private ProblemDetail problema(HttpStatus estado, String slug, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setType(URI.create(BASE_TIPO + slug));
        problema.setTitle(estado.getReasonPhrase());
        return problema;
    }
}
