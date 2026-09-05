package pe.edu.unc.elmirador.unidades.services;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unc.elmirador.unidades.dto.internal.request.ReportarFallaRequest;
import pe.edu.unc.elmirador.unidades.dto.internal.request.ReportarKilometrajeRequest;
import pe.edu.unc.elmirador.unidades.dto.internal.response.ElegibilidadUnidadResponse;
import pe.edu.unc.elmirador.unidades.dto.internal.response.FallaRegistradaResponse;
import pe.edu.unc.elmirador.unidades.dto.internal.response.KilometrajeRegistradoResponse;
import pe.edu.unc.elmirador.unidades.dto.response.ResultadoIdempotente;
import pe.edu.unc.elmirador.unidades.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.unidades.models.entity.PeticionIdempotente;
import pe.edu.unc.elmirador.unidades.models.entity.Unidad;
import pe.edu.unc.elmirador.unidades.models.vo.Kilometraje;
import pe.edu.unc.elmirador.unidades.models.vo.TipoDeCarga;
import pe.edu.unc.elmirador.unidades.repositories.PeticionIdempotenteRepository;
import pe.edu.unc.elmirador.unidades.repositories.UnidadRepository;

@Service
public class UnidadInternalService {

    private final UnidadRepository unidadRepository;
    private final PeticionIdempotenteRepository peticionIdempotenteRepository;
    private final Clock reloj;

    public UnidadInternalService(
            UnidadRepository unidadRepository,
            PeticionIdempotenteRepository peticionIdempotenteRepository,
            Clock reloj) {
        this.unidadRepository = unidadRepository;
        this.peticionIdempotenteRepository = peticionIdempotenteRepository;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public ElegibilidadUnidadResponse elegibilidad(
            String unidadId,
            OffsetDateTime desde,
            OffsetDateTime hasta,
            Integer pesoKg,
            BigDecimal volumenM3,
            TipoDeCarga tipoCargaRequerido) {
        
        Unidad unidad = buscar(unidadId);
        LocalDate fechaEvaluacion = LocalDate.now(reloj);

        List<String> motivos = unidad.motivosDeNoElegibilidad(fechaEvaluacion, pesoKg, volumenM3, tipoCargaRequerido);

        return new ElegibilidadUnidadResponse(
                unidad.getId(),
                motivos.isEmpty(),
                motivos,
                new ElegibilidadUnidadResponse.CapacidadDto(
                        unidad.getCapacidad().pesoMaximoKg(),
                        unidad.getCapacidad().volumenMaximoM3()
                ),
                unidad.getTipo().name(),
                unidad.getEstadoOperativo().situacion().name()
        );
    }

    @Transactional
    public ResultadoIdempotente<KilometrajeRegistradoResponse> reportarKilometraje(
            String unidadId, String clave, ReportarKilometrajeRequest peticion) {
        
        Optional<PeticionIdempotente> yaVista = peticionIdempotenteRepository.findById(clave);
        if (yaVista.isPresent()) {
            return new ResultadoIdempotente<>(new KilometrajeRegistradoResponse(unidadId, peticion.viajeId()), true);
        }

        Unidad unidad = buscar(unidadId);
        unidad.actualizarKilometraje(new Kilometraje(peticion.kilometraje()));
        
        unidadRepository.save(unidad);
        peticionIdempotenteRepository.save(new PeticionIdempotente(clave, unidadId, OffsetDateTime.now(reloj)));
        
        return new ResultadoIdempotente<>(new KilometrajeRegistradoResponse(unidadId, peticion.viajeId()), false);
    }

    @Transactional
    public ResultadoIdempotente<FallaRegistradaResponse> reportarFalla(
            String unidadId, String clave, ReportarFallaRequest peticion) {
        
        Optional<PeticionIdempotente> yaVista = peticionIdempotenteRepository.findById(clave);
        if (yaVista.isPresent()) {
            return new ResultadoIdempotente<>(new FallaRegistradaResponse(unidadId, peticion.viajeId()), true);
        }

        Unidad unidad = buscar(unidadId);
        if (Boolean.TRUE.equals(peticion.dejaInoperativa())) {
            unidad.marcarInoperativa(peticion.descripcion());
        }

        unidadRepository.save(unidad);
        peticionIdempotenteRepository.save(new PeticionIdempotente(clave, unidadId, OffsetDateTime.now(reloj)));
        
        return new ResultadoIdempotente<>(new FallaRegistradaResponse(unidadId, peticion.viajeId()), false);
    }

    private Unidad buscar(String id) {
        return unidadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Unidad", id));
    }
}
