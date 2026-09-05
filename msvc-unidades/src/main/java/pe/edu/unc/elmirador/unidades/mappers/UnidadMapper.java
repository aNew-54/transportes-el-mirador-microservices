package pe.edu.unc.elmirador.unidades.mappers;

import java.util.List;

import pe.edu.unc.elmirador.unidades.dto.response.DocumentoVehicularResponse;
import pe.edu.unc.elmirador.unidades.dto.response.UnidadResponse;
import pe.edu.unc.elmirador.unidades.models.entity.DocumentoVehicular;
import pe.edu.unc.elmirador.unidades.models.entity.Unidad;

public final class UnidadMapper {

    private UnidadMapper() {
    }

    public static UnidadResponse aResponse(Unidad unidad) {
        List<DocumentoVehicularResponse> documentos = unidad.getDocumentos().stream()
                .map(UnidadMapper::aResponse)
                .toList();

        return new UnidadResponse(
                unidad.getId(),
                unidad.getPlaca().valor(),
                unidad.getTipo(),
                unidad.getCapacidad().pesoMaximoKg(),
                unidad.getCapacidad().volumenMaximoM3(),
                unidad.getKilometraje().valor(),
                unidad.getEstadoOperativo().situacion(),
                unidad.getEstadoOperativo().motivo(),
                unidad.getProgramaDeMantenimiento().kmUltimoServicio().valor(),
                unidad.getProgramaDeMantenimiento().kmProximoServicio().valor(),
                unidad.getProgramaDeMantenimiento().intervalo(),
                documentos
        );
    }

    private static DocumentoVehicularResponse aResponse(DocumentoVehicular doc) {
        return new DocumentoVehicularResponse(
                doc.getId(),
                doc.getTipo(),
                doc.getVigencia().desde(),
                doc.getVigencia().hasta(),
                doc.getNumero()
        );
    }
}
