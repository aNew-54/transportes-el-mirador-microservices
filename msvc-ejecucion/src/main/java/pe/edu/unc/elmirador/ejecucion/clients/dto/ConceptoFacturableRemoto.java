package pe.edu.unc.elmirador.ejecucion.clients.dto;

public record ConceptoFacturableRemoto(
        String concepto,
        String monto,
        String moneda,
        String detalle
) {
}
