package com.voltera.ingesta.domain.model;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agregado raiz del BC <b>Telemetria · Ingesta</b> (MS Ingesta de telemetria,
 * lamina 02).
 *
 * <p>Abre el canal de ingesta para los medidores habilitados y valida cada
 * {@code LecturaCrudaRecibida}:</p>
 * <ul>
 *   <li><b>ReglaDeValidacion</b>: rango de kWh por intervalo; fuera de rango =&gt; sospechosa.</li>
 *   <li><b>VentanaDeDuplicados</b>: dedup por (medidorSerial, timestamp); una lectura
 *       repetida tras una reconexion no se procesa dos veces.</li>
 *   <li>Solo procesa medidores habilitados ({@code MedidorHabilitado}); los
 *       suspendidos ({@code MedidorSuspendido}) se retiran.</li>
 * </ul>
 */
public class SesionDeIngesta {

    private final ReglaDeValidacion regla;

    /** Medidores habilitados cuyo canal esta abierto (CredencialDispositivo simplificada al serial). */
    private final Set<String> medidoresHabilitados = ConcurrentHashMap.newKeySet();

    /** Ventana de duplicados: claves (medidorSerial|epochMillis) ya vistas. */
    private final Set<String> ventanaDuplicados = ConcurrentHashMap.newKeySet();

    public SesionDeIngesta(ReglaDeValidacion regla) {
        this.regla = regla != null ? regla : new ReglaDeValidacion(0.0, 100.0);
    }

    /** @return true si el canal se abrio por primera vez para este medidor (=> CanalIngestaCreado). */
    public boolean abrirCanal(String medidorSerial) {
        if (medidorSerial == null || medidorSerial.isBlank()) return false;
        return medidoresHabilitados.add(medidorSerial);
    }

    public void cerrarCanal(String medidorSerial) {
        medidoresHabilitados.remove(medidorSerial);
    }

    public boolean canalAbierto(String medidorSerial) {
        return medidoresHabilitados.contains(medidorSerial);
    }

    /**
     * Procesa una lectura cruda recibida y decide su resultado (sin efectos de
     * publicacion; eso lo hace el servicio de aplicacion segun el resultado).
     */
    public ResultadoValidacion procesar(String medidorSerial, double consumoKwh, Instant capturadaEn) {
        if (!canalAbierto(medidorSerial)) {
            return ResultadoValidacion.NO_HABILITADO;
        }
        String claveDup = medidorSerial + "|" + (capturadaEn != null ? capturadaEn.toEpochMilli() : 0L);
        if (!ventanaDuplicados.add(claveDup)) {
            return ResultadoValidacion.DUPLICADA;
        }
        return regla.esValida(consumoKwh)
                ? ResultadoValidacion.VALIDADA
                : ResultadoValidacion.SOSPECHOSA;
    }

    public int totalMedidores() { return medidoresHabilitados.size(); }
    public ReglaDeValidacion regla() { return regla; }
}
