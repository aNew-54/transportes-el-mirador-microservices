package pe.edu.unc.elmirador.programacion.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
import pe.edu.unc.elmirador.programacion.clients.dto.ElegibilidadUnidadRemota;

@FeignClient(name = "unidades", url = "${clients.unidades.url}")
public interface UnidadesClient {
    @GetMapping("/internal/v1/unidades/{unidadId}/elegibilidad")
    ElegibilidadUnidadRemota consultarElegibilidad(
        @PathVariable("unidadId") String unidadId,
        @RequestParam("desde") OffsetDateTime desde,
        @RequestParam("hasta") OffsetDateTime hasta,
        @RequestParam("pesoKg") int pesoKg,
        @RequestParam("volumenM3") BigDecimal volumenM3,
        @RequestParam("tipoCargaRequerido") String tipoCargaRequerido
    );
}
