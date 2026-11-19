package com.voltera.ia.domain.model.gobernanza;

import com.voltera.ia.domain.model.der.Severidad;
import com.voltera.ia.domain.model.der.TipoAccion;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agregado que modela, del lado de la plataforma GenAI, el ciclo de vida de una
 * propuesta de acción correctiva gobernada por el contrato {@code MultiFirmaAccionDER}
 * (3/3). Es el reflejo off-chain de lo que el MCP Blockchain Gateway escribe y
 * lee de la cadena: propuesta → 3 firmas (IA + operador + prosumidor) → ejecución.
 *
 * <p>Reglas invariantes que impone el documento:</p>
 * <ul>
 *   <li>La acción NUNCA se ejecuta sin las 3 firmas (no hay modo 2/3, fail-closed).</li>
 *   <li>Un rechazo de cualquier firmante deja la propuesta RECHAZADA.</li>
 *   <li>Vencido el TTL sin las 3 firmas, la propuesta queda VENCIDA y no se ejecuta.</li>
 *   <li>Cada propuesta guarda el hash del diagnóstico y la versión de prompt+modelo
 *       que la generaron (trazabilidad para la CREG).</li>
 * </ul>
 */
public final class PropuestaAccion {

    private final UUID propuestaId;
    private final UUID diagnosticoId;
    private final String activoId;
    private final String comunidadId;
    private final TipoAccion tipoAccion;
    private final String comando;
    private final String parametros;
    private final Severidad severidad;
    private final String hashDiagnostico;
    private final String versionPrompt;
    private final String versionModelo;
    private final Instant creadaEn;
    private final Instant venceEn;

    private final Map<Firmante, Firma> firmas = new EnumMap<>(Firmante.class);
    private EstadoPropuesta estado = EstadoPropuesta.PENDIENTE;
    private String resultadoEjecucion;
    private final Map<Firmante, String> txPorFirmante = new EnumMap<>(Firmante.class);
    private String txEjecucion;

    private PropuestaAccion(UUID diagnosticoId, String activoId, String comunidadId,
                            TipoAccion tipoAccion, String comando, String parametros,
                            Severidad severidad, String hashDiagnostico,
                            String versionPrompt, String versionModelo, Duration ttl) {
        this.propuestaId = UUID.randomUUID();
        this.diagnosticoId = diagnosticoId;
        this.activoId = activoId;
        this.comunidadId = comunidadId;
        this.tipoAccion = tipoAccion;
        this.comando = comando;
        this.parametros = parametros == null ? "" : parametros;
        this.severidad = severidad;
        this.hashDiagnostico = hashDiagnostico;
        this.versionPrompt = versionPrompt;
        this.versionModelo = versionModelo;
        this.creadaEn = Instant.now();
        this.venceEn = this.creadaEn.plus(ttl);
    }

    /** Crea la propuesta y aplica la Firma 1 (el Agente de Acción firma lo que generó). */
    public static PropuestaAccion crear(UUID diagnosticoId, String activoId, String comunidadId,
                                        TipoAccion tipoAccion, String comando, String parametros,
                                        Severidad severidad, String hashDiagnostico,
                                        String versionPrompt, String versionModelo, Duration ttl) {
        PropuestaAccion p = new PropuestaAccion(diagnosticoId, activoId, comunidadId, tipoAccion,
                comando, parametros, severidad, hashDiagnostico, versionPrompt, versionModelo, ttl);
        p.firmar(Firma.aprobacion(Firmante.AGENTE_IA, "agente-accion@voltera",
                "El sistema de diagnóstico certifica la recomendación con su evidencia."));
        return p;
    }

    /** Registra una firma (aprobación o rechazo) respetando las invariantes 3/3. */
    public void firmar(Firma firma) {
        if (estado != EstadoPropuesta.PENDIENTE) {
            throw new IllegalStateException("La propuesta ya está " + estado + "; no admite más firmas.");
        }
        if (vencida()) {
            estado = EstadoPropuesta.VENCIDA;
            throw new IllegalStateException("La propuesta venció; no admite firmas. Genere una nueva.");
        }
        if (firmas.containsKey(firma.firmante())) {
            throw new IllegalStateException("El firmante " + firma.firmante() + " ya firmó esta propuesta.");
        }
        firmas.put(firma.firmante(), firma);
        if (!firma.aprueba()) {
            estado = EstadoPropuesta.RECHAZADA;     // un solo rechazo cierra la propuesta
            return;
        }
        if (firmasAprobadas() == 3) {
            estado = EstadoPropuesta.APROBADA;       // 3/3 → lista para ejecutar
        }
    }

    /**
     * Ejecuta el comando aprobado. Solo procede desde APROBADA (3/3): es el único
     * punto donde el contrato emite el evento que el MCP Device Management traduce
     * en el comando físico. Fail-closed por diseño.
     */
    public void marcarEjecutada(String resultado) {
        if (estado != EstadoPropuesta.APROBADA) {
            throw new IllegalStateException(
                    "No se ejecuta sin las 3 firmas. Estado actual: " + estado + " (fail-closed).");
        }
        this.resultadoEjecucion = resultado;
        this.estado = EstadoPropuesta.EJECUTADA;
    }

    /** Marca la propuesta como vencida si expiró el TTL sin completar las firmas. */
    public boolean caducarSiVencio() {
        if (estado == EstadoPropuesta.PENDIENTE && vencida()) {
            estado = EstadoPropuesta.VENCIDA;
            return true;
        }
        return false;
    }

    public boolean vencida() {
        return Instant.now().isAfter(venceEn);
    }

    public int firmasAprobadas() {
        return (int) firmas.values().stream().filter(Firma::aprueba).count();
    }

    public String firmasResumen() {
        return firmasAprobadas() + "/3";
    }

    public Firmante siguienteFirmantePendiente() {
        if (firmas.containsKey(Firmante.OPERADOR)) {
            return firmas.containsKey(Firmante.PROSUMIDOR) ? null : Firmante.PROSUMIDOR;
        }
        return Firmante.OPERADOR;
    }

    /** Asocia a la firma de un firmante la referencia de transacción (txHash o demo) devuelta por el gateway. */
    public void registrarTx(Firmante firmante, String tx) {
        if (tx != null) txPorFirmante.put(firmante, tx);
    }

    public void registrarTxEjecucion(String tx) { this.txEjecucion = tx; }

    public String txDe(Firmante firmante) { return txPorFirmante.get(firmante); }

    public String txEjecucion() { return txEjecucion; }

    public UUID propuestaId() { return propuestaId; }
    public UUID diagnosticoId() { return diagnosticoId; }
    public String activoId() { return activoId; }
    public String comunidadId() { return comunidadId; }
    public TipoAccion tipoAccion() { return tipoAccion; }
    public String comando() { return comando; }
    public String parametros() { return parametros; }
    public Severidad severidad() { return severidad; }
    public String hashDiagnostico() { return hashDiagnostico; }
    public String versionPrompt() { return versionPrompt; }
    public String versionModelo() { return versionModelo; }
    public Instant creadaEn() { return creadaEn; }
    public Instant venceEn() { return venceEn; }
    public EstadoPropuesta estado() { return estado; }
    public String resultadoEjecucion() { return resultadoEjecucion; }

    public List<Firma> firmas() {
        return new ArrayList<>(firmas.values());
    }
}
