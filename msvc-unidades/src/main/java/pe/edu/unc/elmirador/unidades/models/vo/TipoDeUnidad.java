package pe.edu.unc.elmirador.unidades.models.vo;

public enum TipoDeUnidad {
    FURGON {
        @Override
        public boolean admite(TipoDeCarga carga) {
            return carga == TipoDeCarga.PALETIZADA || carga == TipoDeCarga.GENERAL;
        }

        @Override
        public CategoriaDeLicencia licenciaRequerida() {
            return CategoriaDeLicencia.A_IIIA;
        }
    },
    PLATAFORMA {
        @Override
        public boolean admite(TipoDeCarga carga) {
            return carga == TipoDeCarga.PALETIZADA || carga == TipoDeCarga.GENERAL;
        }

        @Override
        public CategoriaDeLicencia licenciaRequerida() {
            return CategoriaDeLicencia.A_IIIB;
        }
    },
    CAMA_BAJA {
        @Override
        public boolean admite(TipoDeCarga carga) {
            return carga == TipoDeCarga.GENERAL || carga == TipoDeCarga.MAQUINARIA_PESADA;
        }

        @Override
        public CategoriaDeLicencia licenciaRequerida() {
            return CategoriaDeLicencia.A_IIIC;
        }
    };

    public abstract boolean admite(TipoDeCarga carga);

    public abstract CategoriaDeLicencia licenciaRequerida();
}
