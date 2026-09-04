package pe.edu.unc.elmirador.ejecucion.models.entity;

import pe.edu.unc.elmirador.ejecucion.models.vo.ResultadoDeCheckList;

public class CheckListDeSalida {

    private final String id;
    private final ResultadoDeCheckList resultado;

    public CheckListDeSalida(String id, ResultadoDeCheckList resultado) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id del check-list es obligatorio");
        }
        if (resultado == null) {
            throw new IllegalArgumentException("El resultado del check-list es obligatorio");
        }
        this.id = id.trim();
        this.resultado = resultado;
    }

    public String getId() {
        return id;
    }

    public ResultadoDeCheckList getResultado() {
        return resultado;
    }

    public boolean estaAprobado() {
        return resultado.aprobado();
    }
}
