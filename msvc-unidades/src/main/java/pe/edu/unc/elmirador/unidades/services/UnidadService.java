package pe.edu.unc.elmirador.unidades.services;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unc.elmirador.unidades.dto.request.ActualizarKilometrajeRequest;
import pe.edu.unc.elmirador.unidades.dto.request.CambiarEstadoRequest;
import pe.edu.unc.elmirador.unidades.dto.request.RegistrarDocumentoRequest;
import pe.edu.unc.elmirador.unidades.dto.request.RegistrarFallaRequest;
import pe.edu.unc.elmirador.unidades.dto.request.RegistrarUnidadRequest;
import pe.edu.unc.elmirador.unidades.dto.response.AlertaResponse;
import pe.edu.unc.elmirador.unidades.dto.response.ElegibilidadResponse;
import pe.edu.unc.elmirador.unidades.dto.response.UnidadResponse;
import pe.edu.unc.elmirador.unidades.exceptions.ConflictoDeRecursoException;
import pe.edu.unc.elmirador.unidades.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.unidades.mappers.UnidadMapper;
import pe.edu.unc.elmirador.unidades.models.entity.Unidad;
import pe.edu.unc.elmirador.unidades.models.vo.Capacidad;
import pe.edu.unc.elmirador.unidades.models.vo.EstadoOperativo;
import pe.edu.unc.elmirador.unidades.models.vo.Kilometraje;
import pe.edu.unc.elmirador.unidades.models.vo.PeriodoDeVigencia;
import pe.edu.unc.elmirador.unidades.models.vo.Placa;
import pe.edu.unc.elmirador.unidades.models.vo.ProgramaDeMantenimiento;
import pe.edu.unc.elmirador.unidades.models.vo.SituacionOperativa;
import pe.edu.unc.elmirador.unidades.models.vo.TipoDeCarga;
import pe.edu.unc.elmirador.unidades.repositories.UnidadRepository;

@Service
public class UnidadService {

    private final UnidadRepository unidadRepository;
    private final Clock reloj;

    public UnidadService(UnidadRepository unidadRepository, Clock reloj) {
        this.unidadRepository = unidadRepository;
        this.reloj = reloj;
    }

    @Transactional
    public UnidadResponse registrar(RegistrarUnidadRequest request) {
        if (unidadRepository.findByPlacaValor(request.placa()).isPresent()) {
            throw new ConflictoDeRecursoException("Ya existe una unidad con la placa " + request.placa());
        }

        Kilometraje inicial = new Kilometraje(request.kilometraje());
        ProgramaDeMantenimiento programa = ProgramaDeMantenimiento.of(inicial, request.intervaloMantenimiento());

        Unidad unidad = new Unidad(
                UUID.randomUUID().toString(),
                new Placa(request.placa()),
                request.tipo(),
                new Capacidad(request.pesoMaximoKg(), request.volumenMaximoM3()),
                inicial,
                EstadoOperativo.operativa(),
                programa,
                List.of()
        );
        return UnidadMapper.aResponse(unidadRepository.save(unidad));
    }

    @Transactional(readOnly = true)
    public UnidadResponse porId(String id) {
        return UnidadMapper.aResponse(buscarUnidad(id));
    }

    @Transactional(readOnly = true)
    public List<UnidadResponse> listar(SituacionOperativa situacion) {
        List<Unidad> unidades = situacion == null
                ? unidadRepository.findAll()
                : unidadRepository.findByEstadoOperativoSituacion(situacion);
        return unidades.stream().map(UnidadMapper::aResponse).toList();
    }

    @Transactional
    public UnidadResponse registrarDocumento(String id, RegistrarDocumentoRequest request) {
        Unidad unidad = buscarUnidad(id);
        PeriodoDeVigencia vigencia = new PeriodoDeVigencia(request.desde(), request.hasta());
        unidad.registrarDocumento(request.tipoDocumento(), vigencia, request.numero(), LocalDate.now(reloj));
        return UnidadMapper.aResponse(unidadRepository.save(unidad));
    }

    @Transactional
    public UnidadResponse cambiarEstado(String id, CambiarEstadoRequest request) {
        Unidad unidad = buscarUnidad(id);
        if (request.situacion() == SituacionOperativa.INOPERATIVA) {
            unidad.marcarInoperativa(request.motivo());
        } else if (request.situacion() == SituacionOperativa.EN_TALLER) {
            unidad.marcarEnTaller(request.motivo());
        } else if (request.situacion() == SituacionOperativa.OPERATIVA) {
            unidad.reactivar(LocalDate.now(reloj));
        }
        return UnidadMapper.aResponse(unidadRepository.save(unidad));
    }

    @Transactional(readOnly = true)
    public ElegibilidadResponse verificarElegibilidad(String id, int pesoKg, BigDecimal volumenM3, TipoDeCarga carga) {
        Unidad unidad = buscarUnidad(id);
        List<String> motivos = unidad.motivosDeNoElegibilidad(LocalDate.now(reloj), pesoKg, volumenM3, carga);
        return new ElegibilidadResponse(motivos.isEmpty(), motivos);
    }

    @Transactional
    public void actualizarKilometraje(String id, ActualizarKilometrajeRequest request) {
        Unidad unidad = buscarUnidad(id);
        unidad.actualizarKilometraje(new Kilometraje(request.kilometraje()));
        unidadRepository.save(unidad);
    }

    @Transactional
    public void registrarFalla(String id, RegistrarFallaRequest request) {
        Unidad unidad = buscarUnidad(id);
        if (request.dejaInoperativa()) {
            unidad.marcarInoperativa(request.motivo());
            unidadRepository.save(unidad);
        }
    }

    @Transactional(readOnly = true)
    public List<AlertaResponse> alertas() {
        LocalDate hoy = LocalDate.now(reloj);
        List<AlertaResponse> alertas = new java.util.ArrayList<>();
        
        for (Unidad unidad : unidadRepository.findAll()) {
            for (var doc : unidad.getDocumentos()) {
                if (doc.getVigencia().venceDentroDe(30, hoy)) {
                    alertas.add(new AlertaResponse(
                            unidad.getId(),
                            unidad.getPlaca().valor(),
                            AlertaResponse.TipoDeAlerta.DOCUMENTO_POR_VENCER,
                            doc.getTipo().name(),
                            "El documento vence el " + doc.getVigencia().hasta()
                    ));
                }
            }
            if (unidad.getProgramaDeMantenimiento().requiereAlerta(unidad.getKilometraje())) {
                alertas.add(new AlertaResponse(
                        unidad.getId(),
                        unidad.getPlaca().valor(),
                        AlertaResponse.TipoDeAlerta.MANTENIMIENTO_PROXIMO,
                        "Mantenimiento",
                        "Km actual: " + unidad.getKilometraje().valor() + ", proximo: " + unidad.getProgramaDeMantenimiento().kmProximoServicio().valor()
                ));
            }
        }
        return alertas;
    }

    private Unidad buscarUnidad(String id) {
        return unidadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Unidad", id));
    }
}
