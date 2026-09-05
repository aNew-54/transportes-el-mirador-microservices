package pe.edu.unc.elmirador.cobranza.dto.response;

import java.util.List;

import pe.edu.unc.elmirador.cobranza.models.vo.TramoDeGestion;

public record CarteraGestionResponse(
        TramoDeGestion tramo,
        List<CuentaPorCobrarResponse> cuentas
) {
}
