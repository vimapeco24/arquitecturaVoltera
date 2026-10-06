package com.voltera.telemetriacore.application;

import com.voltera.telemetriacore.domain.model.ConsumoAgregadoVista;
import com.voltera.telemetriacore.domain.model.ConsumoVista;
import com.voltera.telemetriacore.domain.model.SerieDeMedicion;
import com.voltera.telemetriacore.domain.port.out.ConsumoAgregadoRepositoryPort;
import com.voltera.telemetriacore.domain.port.out.ConsumoVistaRepositoryPort;

/**
 * <b>Proyector</b> del lado QUERY (CQRS, lámina 03: caja "Proyector · actualiza
 * vistas"). Separa la responsabilidad de materializar las vistas de lectura del
 * servicio de command ({@link TelemetriaCoreService}).
 *
 * <p>Cada vez que el command cierra un {@code ConsumoNetoIntervalo}, el Proyector
 * actualiza dos vistas en el almacén de lectura:</p>
 * <ul>
 *   <li>{@link ConsumoVista} — detalle por intervalo (una fila por intervalo).</li>
 *   <li>{@link ConsumoAgregadoVista} — acumulado por medidor (total kWh e intervalos).</li>
 * </ul>
 *
 * <p>Es idempotente a nivel de vista por intervalo (misma clave {@code medidor@epoch}
 * sobrescribe), y acumula incrementalmente la vista agregada.</p>
 */
public class ProyectorVista {

    private final ConsumoVistaRepositoryPort vistasPorIntervalo;
    private final ConsumoAgregadoRepositoryPort vistasAgregadas;

    public ProyectorVista(ConsumoVistaRepositoryPort vistasPorIntervalo,
                          ConsumoAgregadoRepositoryPort vistasAgregadas) {
        this.vistasPorIntervalo = vistasPorIntervalo;
        this.vistasAgregadas = vistasAgregadas;
    }

    /** Proyecta el intervalo cerrado en ambas vistas de lectura. */
    public void proyectar(SerieDeMedicion.ConsumoNetoIntervalo neto) {
        // Vista 1: detalle por intervalo.
        vistasPorIntervalo.guardar(ConsumoVista.de(neto));

        // Vista 2: agregado por medidor (incremental).
        ConsumoAgregadoVista actual = vistasAgregadas.porMedidor(neto.medidorSerial())
                .orElseGet(() -> ConsumoAgregadoVista.inicial(neto.medidorSerial()));
        vistasAgregadas.guardar(actual.acumular(neto));
    }
}
