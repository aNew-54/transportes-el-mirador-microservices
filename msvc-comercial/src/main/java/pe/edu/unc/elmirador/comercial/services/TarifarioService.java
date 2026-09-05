package pe.edu.unc.elmirador.comercial.services;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unc.elmirador.comercial.dto.request.RegistrarTarifarioRequest;
import pe.edu.unc.elmirador.comercial.dto.response.TarifarioResponse;
import pe.edu.unc.elmirador.comercial.mappers.TarifarioMapper;
import pe.edu.unc.elmirador.comercial.models.entity.PrecioDeTarifario;
import pe.edu.unc.elmirador.comercial.models.entity.Tarifario;
import pe.edu.unc.elmirador.comercial.models.vo.Dinero;
import pe.edu.unc.elmirador.comercial.models.vo.PeriodoDeVigencia;
import pe.edu.unc.elmirador.comercial.models.vo.Recargo;
import pe.edu.unc.elmirador.comercial.models.vo.Ruta;
import pe.edu.unc.elmirador.comercial.repositories.TarifarioRepository;

@Service
public class TarifarioService {

    private final TarifarioRepository repositorio;
    private final Clock reloj;

    public TarifarioService(TarifarioRepository repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    @Transactional
    public TarifarioResponse publicar(RegistrarTarifarioRequest peticion) {
        LocalDate hoy = LocalDate.now(reloj);

        Tarifario nuevo = new Tarifario(
                UUID.randomUUID().toString(),
                new PeriodoDeVigencia(peticion.vigenteDesde(), peticion.vigenteHasta()),
                List.of(),
                peticion.recargosEstandar() != null
                        ? peticion.recargosEstandar().stream().map(r -> new Recargo(r.tipo(), r.porcentaje())).toList()
                        : List.of()
        );

        if (peticion.precios() != null) {
            peticion.precios().forEach(p -> nuevo.precios().add(new PrecioDeTarifario(
                    UUID.randomUUID().toString(),
                    new Ruta(p.origen(), p.destino(), p.corredor()),
                    p.tipoUnidad(),
                    new Dinero(p.precioMonto(), p.precioMoneda())
            )));
        }

        List<Tarifario> vigentes = repositorio.findAll().stream()
                .filter(t -> t.estaVigenteEn(hoy))
                .toList();

        for (Tarifario vigente : vigentes) {
            nuevo.sucedeA(vigente);
        }

        return TarifarioMapper.aRespuesta(repositorio.save(nuevo));
    }
}
