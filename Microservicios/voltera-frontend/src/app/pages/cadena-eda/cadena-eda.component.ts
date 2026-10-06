import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Página "Cadena EDA" (lámina 04): resumen de la arquitectura orientada a eventos
 * del laboratorio — flujo end-to-end, autoescalado KEDA por lag y event mesh
 * multi-región. Es una vista documentativa que acompaña a las páginas operativas.
 */
@Component({
  selector: 'app-cadena-eda',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './cadena-eda.component.html',
  styleUrl: './domain-page.scss',
})
export class CadenaEdaComponent {
  readonly flujo = [
    { paso: 'Medidor IoT / Head-end', detalle: 'Prosumidores → Adaptador AMI (ACL)' },
    { paso: 'Integración AMI', detalle: 'traduce y emite LecturaCrudaRecibida' },
    { paso: 'Ingesta', detalle: 'valida/deduplica → LecturaValidada / LecturaSospechosa' },
    { paso: 'Telemetría Core (CQRS)', detalle: 'serie append-only → ConsumoIntervaloRegistrado' },
    { paso: 'Tarifas / Liquidación', detalle: 'tarifican y acumulan el consumo' },
    { paso: 'Facturación → Notificaciones', detalle: 'FacturaEmitida → ClienteNotificado' },
  ];

  readonly keda = [
    { comp: 'MS Ingesta', topic: 'ami-lecturas-crudas', umbral: '5000', min: 2, max: 48 },
    { comp: 'Telemetría Core', topic: 'telemetria-lecturas-validadas', umbral: '5000', min: 2, max: 48 },
    { comp: 'Proyector', topic: 'telemetria-consumo-intervalos', umbral: '1000', min: 1, max: 24 },
    { comp: 'Adaptador AMI', topic: 'MQTT pendientes (Prometheus)', umbral: '10000', min: 2, max: 20 },
    { comp: 'Notificaciones', topic: 'telemetria-alertas', umbral: '500', min: 0, max: 30 },
    { comp: 'MS Habilitación', topic: 'ordenes-instalacion', umbral: '50', min: 1, max: 4 },
  ];

  readonly regiones = ['Colombia (principal)', 'Chile', 'México', 'Brasil'];
}
