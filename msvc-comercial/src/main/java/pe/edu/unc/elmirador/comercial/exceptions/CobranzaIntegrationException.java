package pe.edu.unc.elmirador.comercial.exceptions;

/**
 * El contrato 11 no se pudo cumplir: Cobranza no respondio, respondio un error, o respondio algo que
 * este modulo no sabe leer.
 *
 * <p>No hereda de {@link DominioComercialException} a proposito. No es un fallo del dominio comercial
 * y no debe salir por el {@code 422} que ese manejador aplica: el cuerpo de la peticion estaba bien y
 * volver a enviarlo mas tarde puede funcionar. Sale por {@code 503}.
 *
 * <p>Regla 5 de {@code contracts.md}: un fallo remoto nunca se convierte en {@code 404} ni se
 * interpreta como «no existe». Un {@code 404} de Cobranza significa que los dos contextos discrepan
 * sobre que clientes hay, y eso es esta excepcion, no una respuesta vacia.
 */
public class CobranzaIntegrationException extends RuntimeException {

    public CobranzaIntegrationException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    public CobranzaIntegrationException(String mensaje) {
        super(mensaje);
    }
}
