package pe.edu.unc.elmirador.cobranza.models.entity;

import pe.edu.unc.elmirador.cobranza.models.vo.Dinero;

/**
 * Entidad hija del agregado Pago.
 * Representa la aplicacion de una parte o la totalidad del monto de un pago a una cuenta por cobrar.
 */
public class AplicacionDePago {

    private final String id;
    private final String cuentaPorCobrarId;
    private final Dinero importe;

    public AplicacionDePago(String id, String cuentaPorCobrarId, Dinero importe) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id de la aplicacion es obligatorio");
        }
        if (cuentaPorCobrarId == null || cuentaPorCobrarId.isBlank()) {
            throw new IllegalArgumentException("El id de la cuenta por cobrar es obligatorio");
        }
        if (importe == null) {
            throw new IllegalArgumentException("El importe de la aplicacion es obligatorio");
        }
        if (importe.esCero()) {
            throw new IllegalArgumentException("El importe de la aplicacion debe ser mayor a cero");
        }
        this.id = id.trim();
        this.cuentaPorCobrarId = cuentaPorCobrarId.trim();
        this.importe = importe;
    }

    public String id() {
        return id;
    }

    public String cuentaPorCobrarId() {
        return cuentaPorCobrarId;
    }

    public Dinero importe() {
        return importe;
    }
}
