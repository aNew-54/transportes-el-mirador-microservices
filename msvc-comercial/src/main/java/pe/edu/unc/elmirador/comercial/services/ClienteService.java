package pe.edu.unc.elmirador.comercial.services;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unc.elmirador.comercial.dto.request.RegistrarClienteRequest;
import pe.edu.unc.elmirador.comercial.dto.response.ClienteResponse;
import pe.edu.unc.elmirador.comercial.exceptions.ConflictoDeRecursoException;
import pe.edu.unc.elmirador.comercial.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.comercial.mappers.ClienteMapper;
import pe.edu.unc.elmirador.comercial.models.entity.Cliente;
import pe.edu.unc.elmirador.comercial.models.vo.CondicionDePago;
import pe.edu.unc.elmirador.comercial.models.vo.EstadoCrediticio;
import pe.edu.unc.elmirador.comercial.models.vo.RazonSocial;
import pe.edu.unc.elmirador.comercial.models.vo.Ruc;
import pe.edu.unc.elmirador.comercial.models.vo.SituacionCrediticia;
import pe.edu.unc.elmirador.comercial.repositories.ClienteRepository;

@Service
public class ClienteService {

    private final ClienteRepository repositorio;
    private final Clock reloj;

    public ClienteService(ClienteRepository repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    @Transactional
    public ClienteResponse registrar(RegistrarClienteRequest peticion) {
        Ruc ruc = new Ruc(peticion.ruc());

        if (repositorio.findByRucValor(ruc.valor()).isPresent()) {
            throw new ConflictoDeRecursoException("Ya existe un cliente con el RUC " + ruc.valor());
        }

        LocalDate hoy = LocalDate.now(reloj);
        Cliente cliente = new Cliente(
                UUID.randomUUID().toString(),
                ruc,
                new RazonSocial(peticion.razonSocial()),
                new CondicionDePago(peticion.modalidadDePago(), peticion.plazoEnDias()),
                new EstadoCrediticio(SituacionCrediticia.VIGENTE, hoy)
        );

        return ClienteMapper.aRespuesta(repositorio.save(cliente));
    }

    @Transactional(readOnly = true)
    public ClienteResponse porId(String id) {
        return ClienteMapper.aRespuesta(buscar(id));
    }

    private Cliente buscar(String id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", id));
    }
}
