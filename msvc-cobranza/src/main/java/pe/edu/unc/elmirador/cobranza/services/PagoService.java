package pe.edu.unc.elmirador.cobranza.services;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unc.elmirador.cobranza.dto.request.AplicacionRequest;
import pe.edu.unc.elmirador.cobranza.dto.request.AplicarPagoRequest;
import pe.edu.unc.elmirador.cobranza.dto.request.RegistrarPagoRequest;
import pe.edu.unc.elmirador.cobranza.dto.response.PagoResponse;
import pe.edu.unc.elmirador.cobranza.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.cobranza.mappers.PagoMapper;
import pe.edu.unc.elmirador.cobranza.models.entity.CuentaCorrienteDelCliente;
import pe.edu.unc.elmirador.cobranza.models.entity.CuentaPorCobrar;
import pe.edu.unc.elmirador.cobranza.models.entity.Pago;
import pe.edu.unc.elmirador.cobranza.models.vo.Dinero;
import pe.edu.unc.elmirador.cobranza.models.vo.MedioDePago;
import pe.edu.unc.elmirador.cobranza.repositories.CuentaCorrienteDelClienteRepository;
import pe.edu.unc.elmirador.cobranza.repositories.PagoRepository;

@Service
public class PagoService {

    private final PagoRepository pagoRepositorio;
    private final CuentaCorrienteDelClienteRepository cuentaRepositorio;
    private final Clock reloj;

    public PagoService(PagoRepository pagoRepositorio, CuentaCorrienteDelClienteRepository cuentaRepositorio, Clock reloj) {
        this.pagoRepositorio = pagoRepositorio;
        this.cuentaRepositorio = cuentaRepositorio;
        this.reloj = reloj;
    }

    @Transactional
    public PagoResponse registrar(RegistrarPagoRequest peticion) {
        LocalDate hoy = LocalDate.now(reloj);
        Pago pago = new Pago(
                UUID.randomUUID().toString(),
                peticion.clienteId(),
                new Dinero(peticion.montoMonto(), peticion.montoMoneda()),
                new MedioDePago(peticion.modalidad(), peticion.referencia()),
                hoy
        );
        return PagoMapper.aRespuesta(pagoRepositorio.save(pago));
    }

    @Transactional
    public PagoResponse aplicarPago(String id, AplicarPagoRequest peticion) {
        Pago pago = pagoRepositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("pago", id));
        
        CuentaCorrienteDelCliente cliente = cuentaRepositorio.findByClienteId(pago.clienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("cliente", pago.clienteId()));

        for (AplicacionRequest aplicacionReq : peticion.aplicaciones()) {
            CuentaPorCobrar cuenta = cliente.cuentas().stream()
                    .filter(c -> c.id().equals(aplicacionReq.cuentaPorCobrarId()))
                    .findFirst()
                    .orElseThrow(() -> new RecursoNoEncontradoException("cuenta por cobrar", aplicacionReq.cuentaPorCobrarId()));
            
            pago.aplicarACuentaPorCobrar(cuenta, new Dinero(aplicacionReq.importeMonto(), aplicacionReq.importeMoneda()));
        }

        // Evaluar si alguna cuenta paso a suspendido
        // Not requested automatically on payment applied, but payment doesn't age accounts. 
        // Wait, CCC-01: "al cruzar los 30 dias... es automatico". But suspending is evaluated manually or by cron.
        // The evaluation of credit evaluates current status.
        
        cuentaRepositorio.save(cliente);
        return PagoMapper.aRespuesta(pagoRepositorio.save(pago));
    }
}
