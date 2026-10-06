package com.voltera.habilitacion.domain.model;

import java.time.Duration;
import java.time.Instant;

/**
 * Agregado raiz del BC <b>Habilitacion de Medidores</b> (lamina 02).
 *
 * <p>MS Habilitacion actua como <b>mediador del alta</b>: cuando llega
 * {@code OrdenInstalacionCerrada} se crea el Medidor y se emite
 * {@code MedidorHabilitado} con el estado completo. El alta queda
 * {@link EstadoHabilitacion#PENDIENTE} hasta recibir {@code CanalIngestaCreado}
 * (de Ingesta) y {@code TarifaAsignada} (de Tarifas); con ambas pasa a
 * {@link EstadoHabilitacion#ACTIVADO} ({@code MedidorActivado}). Si vence el plazo
 * (15 min) se emite {@code HabilitacionFallida} + {@code MedidorSuspendido}
 * ({@link EstadoHabilitacion#SUSPENDIDO}).</p>
 */
public class Medidor {

    /** Plazo del alta (lamina 01/02: "si vence el plazo se emite HabilitacionFallida"). */
    public static final Duration TIMEOUT_ALTA = Duration.ofMinutes(15);

    private final String medidorId;
    private final IdentidadDispositivo identidad;
    private final PuntoDeMedicion punto;
    private final DatosComerciales datosComerciales;
    private final String ordenInstalacionId;
    private final Instant habilitadoEn;

    private EstadoHabilitacion estado;
    private boolean canalIngestaCreado;
    private boolean tarifaAsignada;
    private Instant finalizadoEn;
    private String motivoFallo;

    private Medidor(String medidorId, IdentidadDispositivo identidad, PuntoDeMedicion punto,
                    DatosComerciales datosComerciales, String ordenInstalacionId, Instant habilitadoEn) {
        this.medidorId = medidorId;
        this.identidad = identidad;
        this.punto = punto;
        this.datosComerciales = datosComerciales == null ? DatosComerciales.porDefecto() : datosComerciales;
        this.ordenInstalacionId = ordenInstalacionId;
        this.habilitadoEn = habilitadoEn;
        this.estado = EstadoHabilitacion.PENDIENTE;
    }

    /**
     * Habilita un medidor a partir del cierre de la orden de instalacion
     * ({@code OrdenInstalacionCerrada}). Queda PENDIENTE esperando confirmaciones.
     */
    public static Medidor habilitar(String medidorId, IdentidadDispositivo identidad,
                                    PuntoDeMedicion punto, DatosComerciales datosComerciales,
                                    String ordenInstalacionId) {
        if (medidorId == null || medidorId.isBlank()) {
            throw new IllegalArgumentException("medidorId es obligatorio");
        }
        if (identidad == null) {
            throw new IllegalArgumentException("La identidad del dispositivo es obligatoria");
        }
        if (punto == null) {
            throw new IllegalArgumentException("El punto de medicion es obligatorio");
        }
        return new Medidor(medidorId, identidad, punto, datosComerciales, ordenInstalacionId, Instant.now());
    }

    /** Sobrecarga de compatibilidad: habilita con datos comerciales por defecto. */
    public static Medidor habilitar(String medidorId, IdentidadDispositivo identidad,
                                    PuntoDeMedicion punto, String ordenInstalacionId) {
        return habilitar(medidorId, identidad, punto, DatosComerciales.porDefecto(), ordenInstalacionId);
    }

    /** Rehidratacion desde persistencia. */
    public static Medidor rehidratar(String medidorId, IdentidadDispositivo identidad,
                                     PuntoDeMedicion punto, DatosComerciales datosComerciales,
                                     String ordenInstalacionId,
                                     Instant habilitadoEn, EstadoHabilitacion estado,
                                     boolean canalIngestaCreado, boolean tarifaAsignada,
                                     Instant finalizadoEn, String motivoFallo) {
        Medidor m = new Medidor(medidorId, identidad, punto, datosComerciales, ordenInstalacionId, habilitadoEn);
        m.estado = estado;
        m.canalIngestaCreado = canalIngestaCreado;
        m.tarifaAsignada = tarifaAsignada;
        m.finalizadoEn = finalizadoEn;
        m.motivoFallo = motivoFallo;
        return m;
    }

    /** Confirmacion de Ingesta (CanalIngestaCreado). */
    public void registrarCanalIngestaCreado() {
        if (estado != EstadoHabilitacion.PENDIENTE) return;
        this.canalIngestaCreado = true;
        intentarActivar();
    }

    /** Confirmacion de Tarifas (TarifaAsignada). */
    public void registrarTarifaAsignada() {
        if (estado != EstadoHabilitacion.PENDIENTE) return;
        this.tarifaAsignada = true;
        intentarActivar();
    }

    private void intentarActivar() {
        if (canalIngestaCreado && tarifaAsignada) {
            this.estado = EstadoHabilitacion.ACTIVADO;
            this.finalizadoEn = Instant.now();
        }
    }

    /** Suspension por timeout del plazo (segun el reloj). */
    public boolean suspenderPorTimeout(Instant ahora) {
        if (estado != EstadoHabilitacion.PENDIENTE) return false;
        if (Duration.between(habilitadoEn, ahora).compareTo(TIMEOUT_ALTA) < 0) return false;
        aplicarSuspension();
        return true;
    }

    /** Suspension forzada (demo del timeout sin esperar 15 min). */
    public boolean forzarSuspension() {
        if (estado != EstadoHabilitacion.PENDIENTE) return false;
        aplicarSuspension();
        return true;
    }

    private void aplicarSuspension() {
        this.estado = EstadoHabilitacion.SUSPENDIDO;
        this.finalizadoEn = Instant.now();
        StringBuilder sb = new StringBuilder("Alta no confirmada en 15 min. Falto: ");
        if (!canalIngestaCreado) sb.append("CanalIngestaCreado ");
        if (!tarifaAsignada) sb.append("TarifaAsignada ");
        this.motivoFallo = sb.toString().trim();
    }

    public boolean estaPendiente() { return estado == EstadoHabilitacion.PENDIENTE; }

    // Getters
    public String medidorId() { return medidorId; }
    public IdentidadDispositivo identidad() { return identidad; }
    public PuntoDeMedicion punto() { return punto; }
    public DatosComerciales datosComerciales() { return datosComerciales; }
    public String ordenInstalacionId() { return ordenInstalacionId; }
    public Instant habilitadoEn() { return habilitadoEn; }
    public EstadoHabilitacion estado() { return estado; }
    public boolean canalIngestaCreado() { return canalIngestaCreado; }
    public boolean tarifaAsignada() { return tarifaAsignada; }
    public Instant finalizadoEn() { return finalizadoEn; }
    public String motivoFallo() { return motivoFallo; }
}
