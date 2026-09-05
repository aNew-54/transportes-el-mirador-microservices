package pe.edu.unc.elmirador.programacion.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.math.BigDecimal;
import pe.edu.unc.elmirador.programacion.clients.dto.ElegibilidadUnidadRemota;

@FeignClient(name = "unidades", url = "${clients.unidades.url}")
public interface UnidadesClient {

    /**
     * Las fechas viajan como texto ISO 8601 con offset, no como {@code OffsetDateTime}.
     *
     * <p>No es una comodidad: Feign expande un parametro de consulta con el formateador del
     * <em>locale</em> por defecto del JVM, asi que un {@code OffsetDateTime} salia como
     * {@code 10/10/26, 1:00 p. m.} y el proveedor respondia 400. Ademas de romper la regla 6, ese
     * formato pierde el offset. Formatearlo en la pasarela lo hace explicito y comprobable.
     */
    @GetMapping("/internal/v1/unidades/{unidadId}/elegibilidad")
    ElegibilidadUnidadRemota consultarElegibilidad(
        @PathVariable("unidadId") String unidadId,
        @RequestParam("desde") String desde,
        @RequestParam("hasta") String hasta,
        @RequestParam("pesoKg") int pesoKg,
        @RequestParam("volumenM3") BigDecimal volumenM3,
        @RequestParam("tipoCargaRequerido") String tipoCargaRequerido
    );
}
