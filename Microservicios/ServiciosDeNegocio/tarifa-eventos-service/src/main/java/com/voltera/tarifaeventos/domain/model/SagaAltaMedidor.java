package com.voltera.tarifaeventos.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Agregado de la <b>saga de alta de medidor</b> — patron Orquestacion (mediador),
 * lamina 10.
 *
 * <p>Un <i>process manager</i> dirige el alta: publica {@code MedidorHabilitado} y
 * queda esperando DOS respuestas de servicios colaboradores:</p>
 * <ul>
 *   <li>{@code CanalIngestaCreado} (lo crea el servicio de Ingesta/Adaptador)</li>
 *   <li>{@code TarifaAsignada} (lo crea el servicio de Tarifas)</li>
 * </ul>
 *
 * <p>Si ambas llegan dentro de la ventana de tiempo, la saga queda
 * {@link EstadoSaga#COMPLETADA}. Si vence el <b>timeout de 15 min</b> sin ambas
 * respuestas, el process manager compensa: emite {@code HabilitacionFallida} y
 * {@code MedidorSuspendido} y la saga queda {@link EstadoSaga#FALLIDA}. Esto protege
 * la <i>trazabilidad y consistencia del proceso de alta</i>.</p>
 */
public class SagaAltaMedidor {

    /** Timeout de negocio del alta (lamina 10: "Si vencen 15 min..."). */
    public static final Duration TIMEOUT = Duration.ofMinutes(15);

    private final String sagaId;
    private final String medidorId;
    private final String prosumidorId;
    private final Instant iniciadaEn;

    private EstadoSaga estado;
    private boolean canalIngestaCreado;
    private boolean tarifaAsignada;
    private Instant finalizadaEn;
    private String motivoFallo;

    private SagaAltaMedidor(String sagaId, String medidorId, String prosumidorId, Instant iniciadaEn) {
        this.sagaId = sagaId;
        this.medidorId = medidorId;
        this.prosumidorId = prosumidorId;
        this.iniciadaEn = iniciadaEn;
        this.estado = EstadoSaga.INICIADA;
    }

    /** Arranca una nueva saga de alta (estado INICIADA, esperando colaboradores). */
    public static SagaAltaMedidor iniciar(String medidorId, String prosumidorId) {
        if (medidorId == null || medidorId.isBlank()) {
            throw new IllegalArgumentException("medidorId es obligatorio");
        }
        return new SagaAltaMedidor("saga-" + UUID.randomUUID(), medidorId, prosumidorId, Instant.now());
    }

    /** Rehidratacion desde persistencia. */
    public static SagaAltaMedidor rehidratar(String sagaId, String medidorId, String prosumidorId,
                                             Instant iniciadaEn, EstadoSaga estado,
                                             boolean canalIngestaCreado, boolean tarifaAsignada,
                                             Instant finalizadaEn, String motivoFallo) {
        SagaAltaMedidor s = new SagaAltaMedidor(sagaId, medidorId, prosumidorId, iniciadaEn);
        s.estado = estado;
        s.canalIngestaCreado = canalIngestaCreado;
        s.tarifaAsignada = tarifaAsignada;
        s.finalizadaEn = finalizadaEn;
        s.motivoFallo = motivoFallo;
        return s;
    }

    /** Respuesta del servicio de Ingesta. Si ya estan ambas, completa la saga. */
    public void registrarCanalIngestaCreado() {
        if (estado != EstadoSaga.INICIADA) return;
        this.canalIngestaCreado = true;
        intentarCompletar();
    }

    /** Respuesta del servicio de Tarifas. Si ya estan ambas, completa la saga. */
    public void registrarTarifaAsignada() {
        if (estado != EstadoSaga.INICIADA) return;
        this.tarifaAsignada = true;
        intentarCompletar();
    }

    private void intentarCompletar() {
        if (canalIngestaCreado && tarifaAsignada) {
            this.estado = EstadoSaga.COMPLETADA;
            this.finalizadaEn = Instant.now();
        }
    }

    /**
     * Marca la saga como fallida por timeout. El llamante (process manager) debe
     * emitir las compensaciones HabilitacionFallida + MedidorSuspendido.
     *
     * @return {@code true} si realmente se marco como fallida (seguia INICIADA).
     */
    public boolean marcarFallidaPorTimeout(Instant ahora) {
        if (estado != EstadoSaga.INICIADA) return false;
        if (!haVencido(ahora)) return false;
        this.estado = EstadoSaga.FALLIDA;
        this.finalizadaEn = ahora;
        this.motivoFallo = faltantes();
        return true;
    }

    public boolean haVencido(Instant ahora) {
        return Duration.between(iniciadaEn, ahora).compareTo(TIMEOUT) >= 0;
    }

    /**
     * Fuerza el fallo de la saga sin esperar al reloj (para demostrar la
     * compensacion del timeout en vivo). Solo aplica si sigue INICIADA.
     *
     * @return {@code true} si se marco como fallida.
     */
    public boolean forzarTimeout() {
        if (estado != EstadoSaga.INICIADA) return false;
        this.estado = EstadoSaga.FALLIDA;
        this.finalizadaEn = Instant.now();
        this.motivoFallo = faltantes();
        return true;
    }

    private String faltantes() {
        StringBuilder sb = new StringBuilder("Timeout 15 min. Faltaron respuestas: ");
        if (!canalIngestaCreado) sb.append("CanalIngestaCreado ");
        if (!tarifaAsignada) sb.append("TarifaAsignada ");
        return sb.toString().trim();
    }

    public boolean estaPendiente() {
        return estado == EstadoSaga.INICIADA;
    }

    // Getters
    public String sagaId() { return sagaId; }
    public String medidorId() { return medidorId; }
    public String prosumidorId() { return prosumidorId; }
    public Instant iniciadaEn() { return iniciadaEn; }
    public EstadoSaga estado() { return estado; }
    public boolean canalIngestaCreado() { return canalIngestaCreado; }
    public boolean tarifaAsignada() { return tarifaAsignada; }
    public Instant finalizadaEn() { return finalizadaEn; }
    public String motivoFallo() { return motivoFallo; }
}
