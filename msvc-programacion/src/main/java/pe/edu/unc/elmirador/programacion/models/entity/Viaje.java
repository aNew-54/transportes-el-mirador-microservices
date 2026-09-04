package pe.edu.unc.elmirador.programacion.models.entity;

import java.util.ArrayList;
import java.util.List;
import pe.edu.unc.elmirador.programacion.exceptions.AsignacionIncompletaException;
import pe.edu.unc.elmirador.programacion.exceptions.CapacidadExcedidaException;
import pe.edu.unc.elmirador.programacion.exceptions.CargaIncompatibleException;
import pe.edu.unc.elmirador.programacion.exceptions.ConsolidacionProhibidaException;
import pe.edu.unc.elmirador.programacion.exceptions.CorredorIncompatibleException;
import pe.edu.unc.elmirador.programacion.exceptions.DominioProgramacionException;
import pe.edu.unc.elmirador.programacion.exceptions.TransicionDeViajeInvalidaException;
import pe.edu.unc.elmirador.programacion.exceptions.ViajeDespachadoException;
import pe.edu.unc.elmirador.programacion.models.vo.AsignacionDeRecursos;
import pe.edu.unc.elmirador.programacion.models.vo.Capacidad;
import pe.edu.unc.elmirador.programacion.models.vo.Carga;
import pe.edu.unc.elmirador.programacion.models.vo.CargaConsolidada;
import pe.edu.unc.elmirador.programacion.models.vo.ClausulaDeConsolidacion;
import pe.edu.unc.elmirador.programacion.models.vo.EstadoDeViaje;
import pe.edu.unc.elmirador.programacion.models.vo.HojaDeRuta;
import pe.edu.unc.elmirador.programacion.models.vo.Ruta;
import pe.edu.unc.elmirador.programacion.models.vo.VentanaDeTiempo;

public class Viaje {

    private final String id;
    private final Ruta ruta;
    private final VentanaDeTiempo ventana;
    private CargaConsolidada cargaConsolidada;
    private AsignacionDeRecursos asignacionDeRecursos;
    private EstadoDeViaje estado;
    private HojaDeRuta hojaDeRuta;
    private final List<String> ordenIds;

    public Viaje(
            String id,
            Ruta ruta,
            VentanaDeTiempo ventana,
            CargaConsolidada cargaConsolidada,
            List<String> ordenIds) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id del viaje es obligatorio");
        }
        if (ruta == null) {
            throw new IllegalArgumentException("La ruta es obligatoria");
        }
        if (ventana == null) {
            throw new IllegalArgumentException("La ventana de tiempo es obligatoria");
        }
        if (cargaConsolidada == null) {
            throw new IllegalArgumentException("La carga consolidada es obligatoria");
        }
        if (ordenIds == null || ordenIds.isEmpty()) {
            throw new IllegalArgumentException("La lista de ordenes iniciales no puede estar vacia");
        }

        this.id = id.trim();
        this.ruta = ruta;
        this.ventana = ventana;
        this.cargaConsolidada = cargaConsolidada;
        this.ordenIds = new ArrayList<>(ordenIds);
        this.estado = EstadoDeViaje.PLANIFICADO;
        this.asignacionDeRecursos = null;
        this.hojaDeRuta = null;
    }

    public static Viaje planificar(String id, Ruta ruta, VentanaDeTiempo ventana, Carga cargaInicial) {
        if (cargaInicial == null) {
            throw new IllegalArgumentException("La carga inicial es obligatoria");
        }
        return new Viaje(
                id,
                ruta,
                ventana,
                CargaConsolidada.de(cargaInicial),
                List.of(cargaInicial.ordenDeServicioId())
        );
    }

    public static Viaje planificar(
            String id,
            Ruta ruta,
            VentanaDeTiempo ventana,
            CargaConsolidada cargaConsolidada,
            List<String> ordenIds) {
        return new Viaje(id, ruta, ventana, cargaConsolidada, ordenIds);
    }

    public String id() {
        return id;
    }

    public Ruta ruta() {
        return ruta;
    }

    public VentanaDeTiempo ventana() {
        return ventana;
    }

    public CargaConsolidada cargaConsolidada() {
        return cargaConsolidada;
    }

    public AsignacionDeRecursos asignacionDeRecursos() {
        return asignacionDeRecursos;
    }

    public EstadoDeViaje estado() {
        return estado;
    }

    public HojaDeRuta hojaDeRuta() {
        return hojaDeRuta;
    }

    public List<String> ordenIds() {
        return List.copyOf(ordenIds);
    }

    public void asignarRecursos(AsignacionDeRecursos asignacion) {
        if (asignacion == null) {
            throw new IllegalArgumentException("La asignacion de recursos es obligatoria");
        }
        if (this.estado == EstadoDeViaje.DESPACHADO) {
            throw new ViajeDespachadoException("No se pueden asignar recursos a un viaje despachado: " + id);
        }
        if (this.estado == EstadoDeViaje.CANCELADO) {
            throw new DominioProgramacionException("No se pueden asignar recursos a un viaje cancelado: " + id);
        }
        this.asignacionDeRecursos = asignacion;
    }

    public void confirmarProgramacion(HojaDeRuta hojaDeRuta) {
        if (hojaDeRuta == null) {
            throw new IllegalArgumentException("La hoja de ruta es obligatoria");
        }
        if (!this.estado.puedeTransicionarA(EstadoDeViaje.PROGRAMADO)) {
            throw new TransicionDeViajeInvalidaException(
                "No se puede programar un viaje en estado " + this.estado + ": " + id
            );
        }
        if (this.asignacionDeRecursos == null || !this.asignacionDeRecursos.esCompleta()) {
            throw new AsignacionIncompletaException(
                "VIA-01: El viaje no puede programarse sin unidad y al menos un conductor asignados: " + id
            );
        }
        this.hojaDeRuta = hojaDeRuta;
        this.estado = EstadoDeViaje.PROGRAMADO;
    }

    public void autorizarDespacho() {
        if (!this.estado.puedeTransicionarA(EstadoDeViaje.DESPACHADO)) {
            throw new TransicionDeViajeInvalidaException(
                "No se puede despachar un viaje en estado " + this.estado + ": " + id
            );
        }
        this.estado = EstadoDeViaje.DESPACHADO;
    }

    public void cancelar() {
        if (!this.estado.puedeTransicionarA(EstadoDeViaje.CANCELADO)) {
            throw new TransicionDeViajeInvalidaException(
                "No se puede cancelar un viaje en estado " + this.estado + ": " + id
            );
        }
        this.estado = EstadoDeViaje.CANCELADO;
    }

    public void consolidarOrden(
            Carga carga,
            Ruta rutaDeLaOrden,
            VentanaDeTiempo ventanaDeLaOrden,
            ClausulaDeConsolidacion clausulaDelContrato,
            Capacidad capacidadDeLaUnidad) {
        // VIA-07: Comprobacion mandatoria en S1a
        if (this.estado == EstadoDeViaje.DESPACHADO) {
            throw new ViajeDespachadoException(
                "VIA-07: Un viaje despachado no admite nuevas ordenes: " + id
            );
        }
        if (this.estado == EstadoDeViaje.CANCELADO) {
            throw new DominioProgramacionException(
                "No se pueden consolidar ordenes en un viaje cancelado: " + id
            );
        }
        if (carga == null) {
            throw new IllegalArgumentException("La carga es obligatoria");
        }
        if (rutaDeLaOrden == null) {
            throw new IllegalArgumentException("La ruta de la orden es obligatoria");
        }
        if (ventanaDeLaOrden == null) {
            throw new IllegalArgumentException("La ventana de tiempo de la orden es obligatoria");
        }
        if (clausulaDelContrato == null) {
            throw new IllegalArgumentException("La clausula de consolidacion es obligatoria");
        }
        if (capacidadDeLaUnidad == null) {
            throw new IllegalArgumentException("La capacidad de la unidad es obligatoria");
        }

        // Se comprueba TODO antes de mutar NADA (regla D6): una consolidacion rechazada no puede
        // dejar el viaje con la carga a medio agregar. El orden va de lo barato a lo caro, y la
        // capacidad queda al final porque obliga a recorrer la lista entera dos veces.

        // VIA-04: el contrato marco de la orden manda sobre la decision del planificador.
        if (!clausulaDelContrato.permitida()) {
            throw new ConsolidacionProhibidaException(
                "VIA-04: el contrato marco de la orden " + carga.ordenDeServicioId()
                    + " prohibe consolidarla con otras: " + id
            );
        }

        // VIA-03: mismo corredor y ventanas compatibles. Son dos condiciones, no una.
        if (!this.ruta.mismoCorredorQue(rutaDeLaOrden)) {
            throw new CorredorIncompatibleException(
                "VIA-03: la orden " + carga.ordenDeServicioId() + " va por el corredor "
                    + rutaDeLaOrden.corredor() + " y el viaje por " + this.ruta.corredor()
            );
        }
        if (!this.ventana.seSolapaCon(ventanaDeLaOrden)) {
            throw new CorredorIncompatibleException(
                "VIA-03: la ventana de la orden " + carga.ordenDeServicioId()
                    + " no se solapa con la del viaje " + id
            );
        }

        // VIA-05: contra TODAS las cargas ya consolidadas, no solo contra la ultima. Una tercera
        // carga puede ser compatible con la segunda e incompatible con la primera.
        for (Carga yaConsolidada : this.cargaConsolidada.cargas()) {
            if (!yaConsolidada.esCompatibleCon(carga)) {
                throw new CargaIncompatibleException(
                    "VIA-05: la carga " + carga.tipo() + " de la orden " + carga.ordenDeServicioId()
                        + " no es fisicamente compatible con la carga " + yaConsolidada.tipo()
                        + " de la orden " + yaConsolidada.ordenDeServicioId()
                );
            }
        }

        // VIA-02: se evalua sobre la consolidacion resultante, no sobre la actual.
        CargaConsolidada resultante = this.cargaConsolidada.agregar(carga);
        if (!resultante.cabeEn(capacidadDeLaUnidad)) {
            throw new CapacidadExcedidaException(
                "VIA-02: consolidar la orden " + carga.ordenDeServicioId() + " daria "
                    + resultante.pesoTotal() + " kg y " + resultante.volumenTotal()
                    + " m3, y la unidad admite " + capacidadDeLaUnidad.pesoMaximoKg() + " kg y "
                    + capacidadDeLaUnidad.volumenMaximoM3() + " m3"
            );
        }

        this.cargaConsolidada = resultante;
        if (!this.ordenIds.contains(carga.ordenDeServicioId())) {
            this.ordenIds.add(carga.ordenDeServicioId());
        }
    }
}
