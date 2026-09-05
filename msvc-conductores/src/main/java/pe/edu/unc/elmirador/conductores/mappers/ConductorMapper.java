package pe.edu.unc.elmirador.conductores.mappers;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import pe.edu.unc.elmirador.conductores.dto.response.AlertaResponse;
import pe.edu.unc.elmirador.conductores.dto.response.ConductorResponse;
import pe.edu.unc.elmirador.conductores.dto.response.HorasResponse;
import pe.edu.unc.elmirador.conductores.dto.response.InduccionResponse;
import pe.edu.unc.elmirador.conductores.models.entity.Conductor;
import pe.edu.unc.elmirador.conductores.models.entity.Induccion;
import pe.edu.unc.elmirador.conductores.models.vo.HorasDeConduccion;

/**
 * Traduce el agregado a los DTO de respuesta.
 *
 * <p>Va en un solo sentido. La direccion contraria vive en el servicio de aplicacion, porque
 * construir un objeto de valor puede lanzar una excepcion de dominio y esa decision le corresponde
 * al dominio, no a un mapeador.
 */
public final class ConductorMapper {

    private ConductorMapper() {
    }

    public static ConductorResponse aRespuesta(Conductor conductor) {
        return new ConductorResponse(
                conductor.getId(),
                conductor.getNombreCompleto(),
                conductor.getNumeroDeLicencia().valor(),
                conductor.getCategoriaDeLicencia(),
                conductor.getVigenciaLicencia().desde(),
                conductor.getVigenciaLicencia().hasta(),
                conductor.getEstado().situacion(),
                conductor.getEstado().motivo(),
                aHoras(conductor),
                conductor.getInducciones().stream().map(ConductorMapper::aRespuesta).toList()
        );
    }

    public static InduccionResponse aRespuesta(Induccion induccion) {
        return new InduccionResponse(
                induccion.getId(),
                induccion.getClienteId(),
                induccion.getVigencia().desde(),
                induccion.getVigencia().hasta()
        );
    }

    public static HorasResponse aHoras(Conductor conductor) {
        var acumuladas = conductor.getHorasAcumuladas();
        return new HorasResponse(
                conductor.getId(),
                acumuladas.horas(),
                acumuladas.disponibles(),
                HorasDeConduccion.MAXIMO_HORAS,
                acumuladas.ventanaDeComputo().desde(),
                acumuladas.ventanaDeComputo().hasta()
        );
    }

    /**
     * Alertas de un conductor: su licencia y cada una de sus inducciones que vencen dentro del plazo.
     *
     * <p>La decision de si algo vence la toma {@code PeriodoDeVigencia.venceDentroDe}, no este
     * mapeador. Aqui solo se recorre y se da forma.
     */
    public static List<AlertaResponse> alertasDe(Conductor conductor, int dias, LocalDate hoy) {
        List<AlertaResponse> alertas = new ArrayList<>();

        if (conductor.getVigenciaLicencia().venceDentroDe(dias, hoy)) {
            alertas.add(new AlertaResponse(
                    conductor.getId(),
                    conductor.getNombreCompleto(),
                    AlertaResponse.TipoDeAlerta.LICENCIA,
                    conductor.getNumeroDeLicencia().valor(),
                    conductor.getVigenciaLicencia().hasta(),
                    ChronoUnit.DAYS.between(hoy, conductor.getVigenciaLicencia().hasta())
            ));
        }

        for (Induccion induccion : conductor.getInducciones()) {
            if (induccion.getVigencia().venceDentroDe(dias, hoy)) {
                alertas.add(new AlertaResponse(
                        conductor.getId(),
                        conductor.getNombreCompleto(),
                        AlertaResponse.TipoDeAlerta.INDUCCION,
                        induccion.getClienteId(),
                        induccion.getVigencia().hasta(),
                        ChronoUnit.DAYS.between(hoy, induccion.getVigencia().hasta())
                ));
            }
        }

        return alertas;
    }
}
