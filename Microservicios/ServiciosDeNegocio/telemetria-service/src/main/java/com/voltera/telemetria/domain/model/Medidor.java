package com.voltera.telemetria.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Entidad Raiz («root») del agregado Medidor.
 *
 * <p>Modela un medidor inteligente (IoT) de un prosumidor con una maquina de
 * estados: INACTIVO -> ACTIVO -> SUSPENDIDO. Solo un medidor ACTIVO puede
 * registrar lecturas de consumo.</p>
 *
 * <p>El medidor tiene un {@code umbralKwh} contratado. Cuando una lectura supera
 * ese umbral, la lectura se marca como <b>consumo extra</b> (dispara el cargo
 * tarifario adicional en el consumidor).</p>
 */
public class Medidor {

    private final MedidorId id;
    private final String prosumidorId;
    private final ConsumoKwh umbralKwh;
    private EstadoMedidor estado;
    private final Instant registradoEn;

    private Medidor(MedidorId id, String prosumidorId, ConsumoKwh umbralKwh,
                    EstadoMedidor estado, Instant registradoEn) {
        this.id = id;
        this.prosumidorId = prosumidorId;
        this.umbralKwh = umbralKwh;
        this.estado = estado;
        this.registradoEn = registradoEn;
    }

    /** Da de alta un medidor nuevo (nace INACTIVO, pendiente de activacion). */
    public static Medidor registrar(String prosumidorId, ConsumoKwh umbralKwh) {
        validar(prosumidorId, umbralKwh);
        return new Medidor(MedidorId.nuevo(), prosumidorId, umbralKwh,
                EstadoMedidor.INACTIVO, Instant.now());
    }

    public static Medidor reconstituir(MedidorId id, String prosumidorId, ConsumoKwh umbralKwh,
                                       EstadoMedidor estado, Instant registradoEn) {
        return new Medidor(id, prosumidorId, umbralKwh, estado, registradoEn);
    }

    private static void validar(String prosumidorId, ConsumoKwh umbralKwh) {
        if (prosumidorId == null || prosumidorId.isBlank()) {
            throw new IllegalArgumentException("prosumidorId requerido");
        }
        Objects.requireNonNull(umbralKwh, "umbralKwh requerido");
    }

    /** Invariante: activa un medidor INACTIVO o SUSPENDIDO. */
    public void activar() {
        if (estado == EstadoMedidor.ACTIVO) {
            throw new IllegalStateException("El medidor ya esta ACTIVO");
        }
        this.estado = EstadoMedidor.ACTIVO;
    }

    /** Invariante: suspende un medidor ACTIVO. */
    public void suspender() {
        if (estado != EstadoMedidor.ACTIVO) {
            throw new IllegalStateException("Solo un medidor ACTIVO puede SUSPENDERSE");
        }
        this.estado = EstadoMedidor.SUSPENDIDO;
    }

    /**
     * Registra una lectura de consumo y produce el evento de dominio
     * {@link ConsumoRegistrado}. Invariante: solo un medidor ACTIVO puede medir.
     * La lectura se marca como consumo extra si supera el umbral contratado.
     */
    public ConsumoRegistrado registrarLectura(LecturaConsumo lectura) {
        if (estado != EstadoMedidor.ACTIVO) {
            throw new IllegalStateException(
                    "Solo un medidor ACTIVO puede registrar consumo, estado actual: " + estado);
        }
        Objects.requireNonNull(lectura, "lectura requerida");
        boolean extra = lectura.consumo().valor().compareTo(umbralKwh.valor()) > 0;
        return ConsumoRegistrado.desde(this, lectura, extra);
    }

    public MedidorId id() { return id; }
    public String prosumidorId() { return prosumidorId; }
    public ConsumoKwh umbralKwh() { return umbralKwh; }
    public EstadoMedidor estado() { return estado; }
    public Instant registradoEn() { return registradoEn; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Medidor m)) return false;
        return id.equals(m.id);
    }
    @Override public int hashCode() { return id.hashCode(); }
}
