package pe.edu.unc.elmirador.facturacion.services;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unc.elmirador.facturacion.dto.request.EmitirNotaDeCreditoRequest;
import pe.edu.unc.elmirador.facturacion.dto.response.NotaDeCreditoResponse;
import pe.edu.unc.elmirador.facturacion.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.facturacion.mappers.NotaDeCreditoMapper;
import pe.edu.unc.elmirador.facturacion.models.entity.Factura;
import pe.edu.unc.elmirador.facturacion.models.entity.NotaDeCredito;
import pe.edu.unc.elmirador.facturacion.models.vo.Dinero;
import pe.edu.unc.elmirador.facturacion.repositories.FacturaRepository;
import pe.edu.unc.elmirador.facturacion.repositories.NotaDeCreditoRepository;

@Service
public class NotaDeCreditoService {

    private final NotaDeCreditoRepository notaRepository;
    private final FacturaRepository facturaRepository;
    private final Clock reloj;

    public NotaDeCreditoService(NotaDeCreditoRepository notaRepository, FacturaRepository facturaRepository, Clock reloj) {
        this.notaRepository = notaRepository;
        this.facturaRepository = facturaRepository;
        this.reloj = reloj;
    }

    @Transactional
    public NotaDeCreditoResponse emitir(EmitirNotaDeCreditoRequest req) {
        Factura factura = facturaRepository.findById(req.facturaId())
            .orElseThrow(() -> new RecursoNoEncontradoException("factura", req.facturaId()));
        
        Dinero saldoAjustable = factura.saldoAjustable();
        Dinero monto = new Dinero(req.monto(), factura.snapshotComercial().codigoMoneda());

        NotaDeCredito nota = NotaDeCredito.emitir(
            UUID.randomUUID().toString(),
            factura.id(),
            req.motivo(),
            req.motivoDetalle() != null ? req.motivoDetalle() : "",
            monto,
            saldoAjustable,
            OffsetDateTime.now(reloj)
        );

        factura.aplicarNotaDeCredito(nota);

        facturaRepository.save(factura);
        return NotaDeCreditoMapper.aRespuesta(notaRepository.save(nota));
    }
}
