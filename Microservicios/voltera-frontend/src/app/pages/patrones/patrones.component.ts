import { Component, EventEmitter, Input, NgZone, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

type TabPatron = 'cqrs' | 'idempotencia';
type TabTec = 'kafka' | 'event-mesh' | 'keda';

/**
 * Página "Patrones & Tecnología (EDA)".
 *
 * Dos widgets de pestañas con diagramas SVG animados y reactivos, al estilo del
 * material del curso, aterrizados al caso real de Telemetría de Voltera:
 *   1. Patrones: Event Sourcing · CQRS · Idempotencia.
 *   2. Tecnología: Kafka · Event Mesh · KEDA.
 *
 * Los diagramas se mueven solos (animación continua) y además se puede "emitir
 * un evento" para lanzar una partícula que recorre el flujo en tiempo real.
 */
@Component({
  selector: 'app-patrones',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './patrones.component.html',
  styleUrl: './patrones.component.scss',
})
export class PatronesComponent {
  /** Cuando es false, oculta el título de página (para embeber dentro de otra vista). */
  @Input() mostrarHeader = true;

  /** Resultado del flujo REAL que inyecta el padre (banner bajo los botones). */
  @Input() flujoReal: { activo: boolean; texto: string; ok?: boolean } | null = null;

  /** Emite cuando el usuario pulsa "Emitir evento": el padre ejecuta el flujo REAL. */
  @Output() emitirEventoReal = new EventEmitter<void>();

  tabPatron: TabPatron = 'cqrs';
  tabTec: TabTec = 'kafka';

  /** progreso de la partícula-evento recorriendo el diagrama (por widget). */
  playPatron = false;
  playTec = false;

  constructor(private zone: NgZone) {}

  setPatron(t: TabPatron): void {
    this.tabPatron = t;
  }
  setTec(t: TabTec): void {
    this.tabTec = t;
  }

  /** Al emitir: lanza la partícula visual Y pide al padre ejecutar el flujo real de Telemetría. */
  emitirPatron(): void {
    this.playPatron = false;
    requestAnimationFrame(() => {
      this.zone.run(() => (this.playPatron = true));
      setTimeout(() => this.zone.run(() => (this.playPatron = false)), 2600);
    });
    this.emitirEventoReal.emit();
  }

  emitirTec(): void {
    this.playTec = false;
    requestAnimationFrame(() => {
      this.zone.run(() => (this.playTec = true));
      setTimeout(() => this.zone.run(() => (this.playTec = false)), 2600);
    });
    this.emitirEventoReal.emit();
  }
}
