package pe.edu.unc.elmirador.comercial.mappers;

import pe.edu.unc.elmirador.comercial.dto.response.ContratoMarcoResponse;
import pe.edu.unc.elmirador.comercial.models.entity.ContratoMarco;

public final class ContratoMarcoMapper {

    private ContratoMarcoMapper() {
    }

    public static ContratoMarcoResponse aRespuesta(ContratoMarco contrato) {
        return new ContratoMarcoResponse(
                contrato.id(),
                contrato.clienteId(),
                contrato.vigencia().desde(),
                contrato.vigencia().hasta(),
                contrato.tiempoLibre().horas(),
                contrato.clausulaDeConsolidacion().permitida(),
                contrato.clausulaDeConsolidacion().restricciones(),
                contrato.tarifasPactadas().stream().map(t -> new ContratoMarcoResponse.TarifaPactadaResponse(
                        t.id(),
                        t.ruta().origen(),
                        t.ruta().destino(),
                        t.ruta().corredor(),
                        t.tipoDeUnidad().name(),
                        t.precio().monto(),
                        t.precio().codigoMoneda()
                )).toList()
        );
    }
}
