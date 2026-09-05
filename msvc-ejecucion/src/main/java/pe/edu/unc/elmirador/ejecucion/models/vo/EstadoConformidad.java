package pe.edu.unc.elmirador.ejecucion.models.vo;

import pe.edu.unc.elmirador.ejecucion.exceptions.DominioEjecucionException;

public enum EstadoConformidad {
    PENDIENTE,
    FIRMADA,
    OBSERVADA;

    /**
     * El mismo estado dicho en el idioma del contrato 8, que habla de {@code FIRMADA}, {@code PARCIAL}
     * y {@code RECHAZADA}.
     *
     * <p>Los dos vocabularios no coinciden y esa traduccion es justo lo que la capa anticorrupcion
     * existe para hacer. Vive aqui y no en el gateway porque es una regla sobre el significado del
     * estado, no sobre el transporte.
     *
     * <p>{@code PENDIENTE} lanza en vez de traducirse: EJV-03 exige que todas las paradas esten
     * firmadas para marcar la ejecucion como entregada, y solo una ejecucion entregada se cierra,
     * asi que una conformidad pendiente no puede llegar al contrato 8. Si llega, el defecto esta
     * antes y callarlo mandando un {@code RECHAZADA} inventado lo enterraria en Facturacion.
     */
    public String codigoDelContrato() {
        return switch (this) {
            case FIRMADA -> "FIRMADA";
            case OBSERVADA -> "PARCIAL";
            case PENDIENTE -> throw new DominioEjecucionException(
                    "Una conformidad PENDIENTE no se reporta a Facturacion: EJV-03 no deberia haber "
                            + "dejado entregar la ejecucion");
        };
    }
}
