package com.voltera.tarifaeventos.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Fila de la tabla <b>OUTBOX</b> (patron Transactional Outbox, lamina 10).
 *
 * <p>El process manager escribe el cambio de estado de la saga y el evento a
 * publicar <b>en la misma transaccion local</b>. Un <i>relay</i> (poller) lee las
 * filas no publicadas y las envia al broker, marcandolas como publicadas. Asi se
 * evita el problema de "doble escritura" (estado persistido pero evento perdido, o
 * viceversa) sin necesidad de un commit distribuido.</p>
 *
 * @param id          identificador unico de la fila outbox
 * @param tipoEvento  tipo del evento (p. ej. MedidorHabilitado, HabilitacionFallida)
 * @param clave       clave de particion (medidorId): preserva el orden por medidor
 * @param payloadJson cuerpo del evento serializado
 * @param creadoEn    instante de creacion
 * @param publicado   si el relay ya lo entrego al broker
 * @param publicadoEn instante de publicacion (null si pendiente)
 */
public record MensajeOutbox(
        String id,
        String tipoEvento,
        String clave,
        String payloadJson,
        Instant creadoEn,
        boolean publicado,
        Instant publicadoEn
) {
    public static MensajeOutbox pendiente(String tipoEvento, String clave, String payloadJson) {
        return new MensajeOutbox("obx-" + UUID.randomUUID(), tipoEvento, clave, payloadJson,
                Instant.now(), false, null);
    }

    public MensajeOutbox marcarPublicado() {
        return new MensajeOutbox(id, tipoEvento, clave, payloadJson, creadoEn, true, Instant.now());
    }
}
