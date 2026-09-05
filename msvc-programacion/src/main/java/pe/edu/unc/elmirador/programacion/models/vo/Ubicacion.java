package pe.edu.unc.elmirador.programacion.models.vo;

import jakarta.persistence.Embeddable;

/**
 * Donde para el camion. Contrato 4.
 *
 * <p>Hasta {@code S4} esto era un {@code String} suelto dentro de {@link Parada}, y el contrato 4
 * pedia cuatro campos. La forma pobre no daba para entregar: un conductor necesita el distrito para
 * llegar, la referencia para encontrar la puerta y el contacto para que se la abran.
 *
 * <p>Solo {@code direccion} es obligatoria. Un punto de carga habitual puede no tener referencia ni
 * contacto, y exigirlos obligaria a rellenarlos con algo.
 */
@Embeddable
public record Ubicacion(
        String direccion,
        String distrito,
        String referencia,
        String contacto
) {

    public Ubicacion {
        if (direccion == null || direccion.isBlank()) {
            throw new IllegalArgumentException("La direccion de la parada es obligatoria");
        }
        direccion = direccion.trim();
        distrito = normalizar(distrito);
        referencia = normalizar(referencia);
        contacto = normalizar(contacto);
    }

    /** Solo la direccion, cuando es lo unico que se sabe. */
    public static Ubicacion de(String direccion) {
        return new Ubicacion(direccion, null, null, null);
    }

    private static String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}
