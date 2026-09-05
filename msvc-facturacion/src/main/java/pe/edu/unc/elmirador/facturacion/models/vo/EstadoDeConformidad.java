package pe.edu.unc.elmirador.facturacion.models.vo;

/**
 * Como quedó la conformidad del cliente en la parada de descarga. Llega por el contrato 8.
 *
 * <p>Que una conformidad rechazada no cuente como registrada es una regla de negocio —FAC-01 exige
 * conformidad registrada para emitir— y por eso vive aquí. Antes se resolvía con un
 * {@code !"RECHAZADA".equals(estado)} dentro del servicio de aplicación: la misma regla, escrita en un
 * sitio donde nadie la busca y comparando textos sueltos.
 */
public enum EstadoDeConformidad {

    /** El cliente firmó de conformidad. */
    FIRMADA,

    /** Firmó con reparos. Cuenta como registrada; lo que bloquea la emisión son las incidencias. */
    PARCIAL,

    /** El cliente no aceptó la entrega. No hay conformidad que registrar. */
    RECHAZADA;

    public boolean cuentaComoRegistrada() {
        return this != RECHAZADA;
    }
}
