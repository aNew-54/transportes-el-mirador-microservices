package pe.edu.unc.elmirador.programacion.mappers;

import java.util.ArrayList;
import java.util.List;
import pe.edu.unc.elmirador.programacion.dto.response.AsignacionDeRecursosResponse;
import pe.edu.unc.elmirador.programacion.dto.response.CargaConsolidadaResponse;
import pe.edu.unc.elmirador.programacion.dto.response.CargaResponse;
import pe.edu.unc.elmirador.programacion.dto.response.HojaDeRutaResponse;
import pe.edu.unc.elmirador.programacion.dto.response.ParadaResponse;
import pe.edu.unc.elmirador.programacion.dto.response.RutaResponse;
import pe.edu.unc.elmirador.programacion.dto.response.VentanaDeTiempoResponse;
import pe.edu.unc.elmirador.programacion.dto.response.ViajeResponse;
import pe.edu.unc.elmirador.programacion.models.entity.Viaje;
import pe.edu.unc.elmirador.programacion.models.vo.AsignacionDeRecursos;
import pe.edu.unc.elmirador.programacion.models.vo.Carga;
import pe.edu.unc.elmirador.programacion.models.vo.CargaConsolidada;
import pe.edu.unc.elmirador.programacion.models.vo.HojaDeRuta;
import pe.edu.unc.elmirador.programacion.models.vo.Parada;
import pe.edu.unc.elmirador.programacion.models.vo.Ruta;
import pe.edu.unc.elmirador.programacion.models.vo.VentanaDeTiempo;

public final class ViajeMapper {

    private ViajeMapper() {
    }

    public static ViajeResponse aResponse(Viaje viaje) {
        if (viaje == null) return null;
        return new ViajeResponse(
                viaje.id(),
                aResponse(viaje.ruta()),
                aResponse(viaje.ventana()),
                aResponse(viaje.cargaConsolidada()),
                aResponse(viaje.asignacionDeRecursos()),
                viaje.estado(),
                aResponse(viaje.hojaDeRuta()),
                viaje.ordenIds()
        );
    }

    private static RutaResponse aResponse(Ruta ruta) {
        if (ruta == null) return null;
        return new RutaResponse(ruta.origen(), ruta.destino(), ruta.corredor());
    }

    private static VentanaDeTiempoResponse aResponse(VentanaDeTiempo ventana) {
        if (ventana == null) return null;
        return new VentanaDeTiempoResponse(ventana.desde(), ventana.hasta());
    }

    private static CargaConsolidadaResponse aResponse(CargaConsolidada cargaConsolidada) {
        if (cargaConsolidada == null) return null;
        List<CargaResponse> cargas = new ArrayList<>();
        for (Carga c : cargaConsolidada.cargas()) {
            cargas.add(aResponse(c));
        }
        return new CargaConsolidadaResponse(
                cargas,
                cargaConsolidada.pesoTotal(),
                cargaConsolidada.volumenTotal()
        );
    }

    private static CargaResponse aResponse(Carga carga) {
        if (carga == null) return null;
        return new CargaResponse(
                carga.ordenDeServicioId(),
                carga.pesoKg(),
                carga.volumenM3(),
                carga.tipo(),
                carga.secuenciaDeDescarga()
        );
    }

    private static AsignacionDeRecursosResponse aResponse(AsignacionDeRecursos asignacion) {
        if (asignacion == null) return null;
        return new AsignacionDeRecursosResponse(
                asignacion.unidadId(),
                asignacion.conductorIds(),
                asignacion.conRelevo()
        );
    }

    private static HojaDeRutaResponse aResponse(HojaDeRuta hojaDeRuta) {
        if (hojaDeRuta == null) return null;
        List<ParadaResponse> paradas = new ArrayList<>();
        for (Parada p : hojaDeRuta.paradas()) {
            paradas.add(aResponse(p));
        }
        return new HojaDeRutaResponse(paradas);
    }

    private static ParadaResponse aResponse(Parada parada) {
        if (parada == null) return null;
        return new ParadaResponse(
                parada.secuencia(),
                parada.tipo(),
                parada.ordenDeServicioId(),
                parada.ubicacion(),
                parada.horaEstimada()
        );
    }
}
