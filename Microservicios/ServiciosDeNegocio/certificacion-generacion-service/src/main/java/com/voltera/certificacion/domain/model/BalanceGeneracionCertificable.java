package com.voltera.certificacion.domain.model;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Agregado raíz (lámina 04): acumula la generación renovable registrada por
 * Telemetría para un medidor y, cuando hay suficiente, <b>reserva bloques de
 * 1 MWh</b> para certificar, <b>sin reutilizar mediciones</b>.
 *
 * <p>Regla de negocio clave: cada medición (identificada por su id) se consume
 * una sola vez. Un Wh ya certificado no puede volver a certificarse — eso evita
 * el "doble conteo" de energía renovable. El agregado es la frontera de
 * consistencia de esa invariante.</p>
 *
 * <p>Este agregado vive OFF-CHAIN: es estado de dominio del microservicio, no
 * toca la cadena.</p>
 */
public class BalanceGeneracionCertificable {

    private final String medidorId;
    private final long bloqueWh;              // tamaño del bloque certificable (1 MWh por defecto)
    private long generacionAcumuladaWh;       // Wh disponibles aún no reservados
    private long bloquesReservados;           // nº de bloques de bloqueWh ya reservados
    private final Set<String> medicionesUsadas = new LinkedHashSet<>();

    public BalanceGeneracionCertificable(String medidorId, long bloqueWh) {
        if (medidorId == null || medidorId.isBlank()) {
            throw new IllegalArgumentException("medidorId requerido");
        }
        if (bloqueWh <= 0) {
            throw new IllegalArgumentException("bloqueWh debe ser > 0");
        }
        this.medidorId = medidorId;
        this.bloqueWh = bloqueWh;
    }

    /**
     * Registra una medición de generación. Idempotente por {@code medicionId}:
     * si la medición ya se contabilizó, NO se vuelve a sumar (no reutilización).
     * @return true si se contabilizó; false si era un duplicado ignorado.
     */
    public boolean registrarGeneracion(String medicionId, long wh) {
        if (medicionId == null || medicionId.isBlank()) {
            throw new IllegalArgumentException("medicionId requerido");
        }
        if (wh <= 0) {
            throw new IllegalArgumentException("wh debe ser > 0");
        }
        if (medicionesUsadas.contains(medicionId)) {
            return false; // ya usada: no se reutiliza
        }
        medicionesUsadas.add(medicionId);
        generacionAcumuladaWh += wh;
        return true;
    }

    /** ¿Hay energía suficiente para reservar al menos un bloque? */
    public boolean puedeReservarBloque() {
        return generacionAcumuladaWh >= bloqueWh;
    }

    /**
     * Reserva un bloque de {@code bloqueWh} (consume esa energía del acumulado).
     * @throws IllegalStateException si no hay energía suficiente.
     */
    public void reservarBloque() {
        if (!puedeReservarBloque()) {
            throw new IllegalStateException(
                    "Generación insuficiente para reservar un bloque de " + bloqueWh + " Wh (acumulado "
                            + generacionAcumuladaWh + " Wh)");
        }
        generacionAcumuladaWh -= bloqueWh;
        bloquesReservados++;
    }

    public String medidorId() { return medidorId; }
    public long bloqueWh() { return bloqueWh; }
    public long generacionAcumuladaWh() { return generacionAcumuladaWh; }
    public long bloquesReservados() { return bloquesReservados; }
    public int medicionesContabilizadas() { return medicionesUsadas.size(); }
}
