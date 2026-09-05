package pe.edu.unc.elmirador.comercial.services;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unc.elmirador.comercial.dto.request.RegistrarContratoMarcoRequest;
import pe.edu.unc.elmirador.comercial.dto.response.ContratoMarcoResponse;
import pe.edu.unc.elmirador.comercial.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.comercial.mappers.ContratoMarcoMapper;
import pe.edu.unc.elmirador.comercial.models.entity.Cliente;
import pe.edu.unc.elmirador.comercial.models.entity.ContratoMarco;
import pe.edu.unc.elmirador.comercial.models.entity.TarifaPactada;
import pe.edu.unc.elmirador.comercial.models.vo.ClausulaDeConsolidacion;
import pe.edu.unc.elmirador.comercial.models.vo.Dinero;
import pe.edu.unc.elmirador.comercial.models.vo.PeriodoDeVigencia;
import pe.edu.unc.elmirador.comercial.models.vo.Ruta;
import pe.edu.unc.elmirador.comercial.models.vo.TiempoLibre;
import pe.edu.unc.elmirador.comercial.repositories.ClienteRepository;
import pe.edu.unc.elmirador.comercial.repositories.ContratoMarcoRepository;

@Service
public class ContratoMarcoService {

    private final ContratoMarcoRepository contratoRepository;
    private final ClienteRepository clienteRepository;
    private final Clock reloj;

    public ContratoMarcoService(ContratoMarcoRepository contratoRepository, 
                                ClienteRepository clienteRepository, 
                                Clock reloj) {
        this.contratoRepository = contratoRepository;
        this.clienteRepository = clienteRepository;
        this.reloj = reloj;
    }

    @Transactional
    public ContratoMarcoResponse registrar(RegistrarContratoMarcoRequest peticion) {
        Cliente cliente = clienteRepository.findById(peticion.clienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", peticion.clienteId()));

        // Las tarifas se construyen ANTES y entran por el constructor. Anadirlas despues sobre la
        // lista que devuelve el agregado no solo revienta —la devuelve inmutable, y hace bien—,
        // sino que seria construir el agregado por la espalda, sin pasar por sus validaciones.
        List<TarifaPactada> tarifas = new ArrayList<>();
        if (peticion.tarifasPactadas() != null) {
            for (var t : peticion.tarifasPactadas()) {
                tarifas.add(new TarifaPactada(
                        UUID.randomUUID().toString(),
                        new Ruta(t.rutaOrigen(), t.rutaDestino(), t.rutaCorredor()),
                        t.tipoUnidad(),
                        new Dinero(t.precioMonto(), t.precioMoneda())));
            }
        }

        ContratoMarco contrato = new ContratoMarco(
                UUID.randomUUID().toString(),
                cliente.id(),
                new PeriodoDeVigencia(peticion.vigenteDesde(), peticion.vigenteHasta()),
                new TiempoLibre(peticion.tiempoLibreHoras()),
                new ClausulaDeConsolidacion(
                        peticion.consolidacionPermitida(),
                        peticion.consolidacionRestricciones() != null
                                ? peticion.consolidacionRestricciones()
                                : List.of()),
                tarifas);

        return ContratoMarcoMapper.aRespuesta(contratoRepository.save(contrato));
    }
}
