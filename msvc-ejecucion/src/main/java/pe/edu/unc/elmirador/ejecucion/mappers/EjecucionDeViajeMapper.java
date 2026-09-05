package pe.edu.unc.elmirador.ejecucion.mappers;

import pe.edu.unc.elmirador.ejecucion.dto.response.CheckListResponse;
import pe.edu.unc.elmirador.ejecucion.dto.response.ConformidadResponse;
import pe.edu.unc.elmirador.ejecucion.dto.response.EjecucionDeViajeResponse;
import pe.edu.unc.elmirador.ejecucion.dto.response.EsperaFacturableResponse;
import pe.edu.unc.elmirador.ejecucion.dto.response.EvidenciaResponse;
import pe.edu.unc.elmirador.ejecucion.dto.response.HitoResponse;
import pe.edu.unc.elmirador.ejecucion.dto.response.IncidenciaResponse;
import pe.edu.unc.elmirador.ejecucion.dto.response.ParadaResponse;
import pe.edu.unc.elmirador.ejecucion.models.entity.CheckListDeSalida;
import pe.edu.unc.elmirador.ejecucion.models.entity.ConformidadDeEntrega;
import pe.edu.unc.elmirador.ejecucion.models.entity.EjecucionDeViaje;
import pe.edu.unc.elmirador.ejecucion.models.entity.Hito;
import pe.edu.unc.elmirador.ejecucion.models.entity.Incidencia;
import pe.edu.unc.elmirador.ejecucion.models.entity.Parada;
import pe.edu.unc.elmirador.ejecucion.models.vo.EsperaFacturable;
import pe.edu.unc.elmirador.ejecucion.models.vo.Evidencia;

public final class EjecucionDeViajeMapper {

    private EjecucionDeViajeMapper() {
    }

    public static EjecucionDeViajeResponse mapear(EjecucionDeViaje ejecucion) {
        return new EjecucionDeViajeResponse(
                ejecucion.getViajeId(),
                ejecucion.getUnidadEjecutoraId(),
                ejecucion.getEstado(),
                mapear(ejecucion.getCheckList()),
                ejecucion.getParadas().stream().map(EjecucionDeViajeMapper::mapear).toList(),
                ejecucion.getHitos().stream().map(EjecucionDeViajeMapper::mapear).toList(),
                ejecucion.getIncidencias().stream().map(EjecucionDeViajeMapper::mapear).toList(),
                ejecucion.getUnidadesAnteriores()
        );
    }

    private static CheckListResponse mapear(CheckListDeSalida checkList) {
        if (checkList == null || checkList.getResultado() == null) return null;
        return new CheckListResponse(
                checkList.getResultado().aprobado(),
                checkList.getResultado().observaciones(),
                checkList.getResultado().momento()
        );
    }

    private static ParadaResponse mapear(Parada parada) {
        return new ParadaResponse(
                parada.getSecuencia(),
                parada.getOrdenDeServicioId(),
                parada.getDireccion(),
                parada.getEstado(),
                mapear(parada.getConformidad()),
                mapear(parada.getEsperaFacturable())
        );
    }

    private static ConformidadResponse mapear(ConformidadDeEntrega conformidad) {
        if (conformidad == null) return null;
        return new ConformidadResponse(
                conformidad.getEstado(),
                conformidad.getRecibidoPor(),
                conformidad.getMomento(),
                conformidad.getObservaciones()
        );
    }

    private static EsperaFacturableResponse mapear(EsperaFacturable espera) {
        if (espera == null) return null;
        return new EsperaFacturableResponse(
                espera.inicio(),
                espera.fin(),
                espera.tiempoLibreHoras(),
                espera.excedente()
        );
    }

    private static HitoResponse mapear(Hito hito) {
        return new HitoResponse(
                hito.getTipo(),
                hito.getMomento(),
                hito.getUbicacion()
        );
    }

    private static IncidenciaResponse mapear(Incidencia incidencia) {
        return new IncidenciaResponse(
                incidencia.getTipo(),
                incidencia.getDescripcion(),
                mapear(incidencia.getEvidencia()),
                incidencia.isResuelta(),
                incidencia.getMomento()
        );
    }

    private static EvidenciaResponse mapear(Evidencia evidencia) {
        if (evidencia == null) return null;
        return new EvidenciaResponse(
                evidencia.fotografias(),
                evidencia.descripcion(),
                evidencia.momento()
        );
    }
}
