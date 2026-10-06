package com.voltera.notificaciones.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Agregado raiz del BC <b>Notificaciones</b> (lamina 02).
 *
 * <p>El BC Notificaciones es un <b>consumidor puro</b> (solo reacciona a eventos):
 * ante {@code MedidorHabilitado}, {@code LecturaSospechosaDetectada} o
 * {@code MedidorSinReporte} crea una Notificacion dirigida al cliente del medidor,
 * respetando su {@link Preferencia}, y produce el evento {@code ClienteNotificado}.</p>
 */
public class Notificacion {

    private final String notificacionId;
    private final String medidorId;
    private final TipoAlerta tipo;
    private final CanalNotificacion canal;
    private final String mensaje;
    private final Instant generadaEn;

    private Notificacion(String notificacionId, String medidorId, TipoAlerta tipo,
                         CanalNotificacion canal, String mensaje, Instant generadaEn) {
        this.notificacionId = notificacionId;
        this.medidorId = medidorId;
        this.tipo = tipo;
        this.canal = canal;
        this.mensaje = mensaje;
        this.generadaEn = generadaEn;
    }

    /**
     * Crea una notificacion para un medidor segun el tipo de alerta y la
     * preferencia del cliente (que determina el canal de entrega).
     */
    public static Notificacion crear(String medidorId, TipoAlerta tipo, String mensaje,
                                     Preferencia preferencia) {
        if (medidorId == null || medidorId.isBlank()) {
            throw new IllegalArgumentException("medidorId es obligatorio");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de alerta es obligatorio");
        }
        Preferencia pref = preferencia != null ? preferencia : Preferencia.porDefecto();
        return new Notificacion(
                "NTF-" + UUID.randomUUID(),
                medidorId,
                tipo,
                pref.canalPreferido(),
                mensaje != null ? mensaje : textoPorDefecto(tipo, medidorId),
                Instant.now());
    }

    private static String textoPorDefecto(TipoAlerta tipo, String medidorId) {
        return switch (tipo) {
            case MEDIDOR_HABILITADO -> "Su medidor " + medidorId + " fue habilitado correctamente.";
            case LECTURA_SOSPECHOSA -> "Detectamos una lectura inusual en su medidor " + medidorId + ".";
            case MEDIDOR_SIN_REPORTE -> "Su medidor " + medidorId + " dejo de reportar consumo.";
            case FACTURA_EMITIDA -> "Se emitio la factura de su medidor " + medidorId + ".";
        };
    }

    public String notificacionId() { return notificacionId; }
    public String medidorId() { return medidorId; }
    public TipoAlerta tipo() { return tipo; }
    public CanalNotificacion canal() { return canal; }
    public String mensaje() { return mensaje; }
    public Instant generadaEn() { return generadaEn; }
}
