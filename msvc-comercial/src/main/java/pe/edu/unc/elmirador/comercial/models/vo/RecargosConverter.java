package pe.edu.unc.elmirador.comercial.models.vo;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Serializa los recargos de una {@link Tarifa} en una sola columna.
 *
 * <p><strong>Por qué no una {@code @ElementCollection}.</strong> Hibernate no permite redirigir la
 * tabla de una colección declarada dentro de un {@code @Embeddable}: el
 * {@code @AssociationOverride} con {@code joinTable} se ignora, y las tres tarifas del contexto
 * —la de la cotización, la de la orden y el falso flete— caerían todas en la misma tabla por
 * defecto {@code tarifa_recargos}. Una colección dentro de un objeto de valor sólo sirve cuando ese
 * objeto se embebe una única vez.
 *
 * <p>Los recargos de una tarifa son parte del valor de la tarifa y nunca se consultan por separado,
 * así que guardarlos como un texto compacto no pierde nada. El formato es legible en la base:
 * {@code COMBUSTIBLE:10.00;PELIGROSIDAD:5.00}.
 */
@Converter
public class RecargosConverter implements AttributeConverter<List<Recargo>, String> {

    private static final String SEPARADOR_ENTRE = ";";
    private static final String SEPARADOR_DENTRO = ":";

    @Override
    public String convertToDatabaseColumn(List<Recargo> recargos) {
        // El nulo se propaga a proposito. Hibernate decide que un embebido es nulo cuando TODAS sus
        // columnas lo son: si aqui devolvieramos cadena vacia, un falso flete inexistente se
        // materializaria como una Tarifa vacia en vez de como null.
        if (recargos == null) {
            return null;
        }
        if (recargos.isEmpty()) {
            return "";
        }
        StringBuilder texto = new StringBuilder();
        for (Recargo recargo : recargos) {
            if (texto.length() > 0) {
                texto.append(SEPARADOR_ENTRE);
            }
            texto.append(recargo.tipo().name())
                 .append(SEPARADOR_DENTRO)
                 .append(recargo.porcentaje().toPlainString());
        }
        return texto.toString();
    }

    @Override
    public List<Recargo> convertToEntityAttribute(String texto) {
        if (texto == null) {
            return null;
        }
        List<Recargo> recargos = new ArrayList<>();
        if (texto.isBlank()) {
            return recargos;
        }
        for (String parte : texto.split(SEPARADOR_ENTRE)) {
            String[] campos = parte.split(SEPARADOR_DENTRO);
            if (campos.length != 2) {
                throw new IllegalStateException("Recargo mal serializado en la base de datos: " + parte);
            }
            recargos.add(new Recargo(TipoDeRecargo.valueOf(campos[0]), new BigDecimal(campos[1])));
        }
        return recargos;
    }
}
