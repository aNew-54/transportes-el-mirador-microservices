package pe.edu.unc.elmirador.ejecucion.models.entity;

import pe.edu.unc.elmirador.ejecucion.exceptions.DominioEjecucionException;
import pe.edu.unc.elmirador.ejecucion.models.vo.EsperaFacturable;
import pe.edu.unc.elmirador.ejecucion.models.vo.EstadoDeParada;

public class Parada {

    private final int secuencia;
    private final String ordenDeServicioId;
    private final String direccion;
    private EstadoDeParada estado;
    private ConformidadDeEntrega conformidad;
    private EsperaFacturable esperaFacturable;

    public Parada(int secuencia, String ordenDeServicioId, String direccion) {
        if (secuencia <= 0) {
            throw new IllegalArgumentException("La secuencia de parada debe ser positiva: " + secuencia);
        }
        if (ordenDeServicioId == null || ordenDeServicioId.isBlank()) {
            throw new IllegalArgumentException("La orden de servicio es obligatoria");
        }
        if (direccion == null || direccion.isBlank()) {
            throw new IllegalArgumentException("La direccion es obligatoria");
        }
        this.secuencia = secuencia;
        this.ordenDeServicioId = ordenDeServicioId.trim();
        this.direccion = direccion.trim();
        this.estado = EstadoDeParada.PENDIENTE;
        this.conformidad = null;
        this.esperaFacturable = null;
    }

    public int getSecuencia() {
        return secuencia;
    }

    public String getOrdenDeServicioId() {
        return ordenDeServicioId;
    }

    public String getDireccion() {
        return direccion;
    }

    public EstadoDeParada getEstado() {
        return estado;
    }

    public ConformidadDeEntrega getConformidad() {
        return conformidad;
    }

    public EsperaFacturable getEsperaFacturable() {
        return esperaFacturable;
    }

    public void registrarConformidad(ConformidadDeEntrega conformidad) {
        if (conformidad == null) {
            throw new IllegalArgumentException("La conformidad es obligatoria");
        }
        if (this.conformidad != null || this.estado == EstadoDeParada.ATENDIDA) {
            throw new DominioEjecucionException("La parada ya cuenta con una conformidad registrada (EJV-02)");
        }
        if (!this.ordenDeServicioId.equals(conformidad.getOrdenDeServicioId())) {
            throw new DominioEjecucionException(
                "La orden de servicio de la conformidad (" + conformidad.getOrdenDeServicioId()
                    + ") no coincide con la parada (" + this.ordenDeServicioId + ") (EJV-02)"
            );
        }
        this.conformidad = conformidad;
        this.estado = EstadoDeParada.ATENDIDA;
    }

    public void registrarEsperaFacturable(EsperaFacturable esperaFacturable) {
        if (esperaFacturable == null) {
            throw new IllegalArgumentException("La espera facturable es obligatoria");
        }
        this.esperaFacturable = esperaFacturable;
    }

    public void llegarASitio() {
        if (this.estado == EstadoDeParada.ATENDIDA) {
            throw new DominioEjecucionException("La parada ya fue atendida");
        }
        this.estado = EstadoDeParada.EN_SITIO;
    }

    public void reabrir() {
        if (this.estado != EstadoDeParada.ATENDIDA) {
            throw new DominioEjecucionException("Solo se pueden reabrir paradas que esten atendidas");
        }
        this.estado = EstadoDeParada.EN_SITIO;
    }

    public boolean tieneConformidadFirmada() {
        return conformidad != null && conformidad.estaFirmada();
    }
}
