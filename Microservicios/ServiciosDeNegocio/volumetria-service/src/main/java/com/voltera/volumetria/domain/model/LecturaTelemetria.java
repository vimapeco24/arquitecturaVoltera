package com.voltera.volumetria.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Entidad Raiz («root») del agregado LecturaTelemetria.
 * Encapsula la ingesta y validacion de una lectura de un medidor AMI.
 * La validacion es la regla de negocio central de este bounded context.
 */
public class LecturaTelemetria {

    // Umbral de negocio: una lectura de mas de 500 kWh en un intervalo de 15 min
    // es fisicamente improbable en un hogar => se marca SOSPECHOSA.
    private static final BigDecimal UMBRAL_SOSPECHOSA_KWH = BigDecimal.valueOf(500);

    private final LecturaId id;
    private final MedidorId medidorId;
    private final Medida medida;
    private final Instant capturadaEn;
    private final EstadoLectura estado;

    private LecturaTelemetria(LecturaId id, MedidorId medidorId, Medida medida,
                              Instant capturadaEn, EstadoLectura estado) {
        this.id = id;
        this.medidorId = medidorId;
        this.medida = medida;
        this.capturadaEn = capturadaEn;
        this.estado = estado;
    }

    /**
     * Factory de dominio: ingesta y valida una lectura.
     * Invariantes:
     *  - timestamp no puede ser futuro => RECHAZADA
     *  - valores atipicos (> umbral) => SOSPECHOSA
     *  - en otro caso => VALIDA
     */
    public static LecturaTelemetria ingestar(MedidorId medidorId, Medida medida, Instant capturadaEn) {
        Objects.requireNonNull(medidorId, "medidorId requerido");
        Objects.requireNonNull(medida, "medida requerida");
        Objects.requireNonNull(capturadaEn, "capturadaEn requerido");

        EstadoLectura estado;
        if (capturadaEn.isAfter(Instant.now())) {
            estado = EstadoLectura.RECHAZADA;
        } else if (medida.kwh().compareTo(UMBRAL_SOSPECHOSA_KWH) > 0) {
            estado = EstadoLectura.SOSPECHOSA;
        } else {
            estado = EstadoLectura.VALIDA;
        }

        return new LecturaTelemetria(LecturaId.nuevo(), medidorId, medida, capturadaEn, estado);
    }

    /**
     * Factory de dominio: ingesta y valida una lectura CONSERVANDO el id dado.
     * Aplica exactamente las mismas invariantes que {@link #ingestar}.
     */
    public static LecturaTelemetria ingestarCon(LecturaId id, MedidorId medidorId, Medida medida, Instant capturadaEn) {
        Objects.requireNonNull(id, "id requerido");
        Objects.requireNonNull(medidorId, "medidorId requerido");
        Objects.requireNonNull(medida, "medida requerida");
        Objects.requireNonNull(capturadaEn, "capturadaEn requerido");

        EstadoLectura estado;
        if (capturadaEn.isAfter(Instant.now())) {
            estado = EstadoLectura.RECHAZADA;
        } else if (medida.kwh().compareTo(UMBRAL_SOSPECHOSA_KWH) > 0) {
            estado = EstadoLectura.SOSPECHOSA;
        } else {
            estado = EstadoLectura.VALIDA;
        }

        return new LecturaTelemetria(id, medidorId, medida, capturadaEn, estado);
    }

    public static LecturaTelemetria reconstituir(LecturaId id, MedidorId medidorId, Medida medida,
                                                 Instant capturadaEn, EstadoLectura estado) {
        return new LecturaTelemetria(id, medidorId, medida, capturadaEn, estado);
    }

    public boolean esValida() {
        return estado == EstadoLectura.VALIDA;
    }

    public LecturaId id() { return id; }
    public MedidorId medidorId() { return medidorId; }
    public Medida medida() { return medida; }
    public Instant capturadaEn() { return capturadaEn; }
    public EstadoLectura estado() { return estado; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LecturaTelemetria l)) return false;
        return id.equals(l.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
