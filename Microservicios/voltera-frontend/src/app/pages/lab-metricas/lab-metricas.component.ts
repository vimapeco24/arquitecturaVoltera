import { Component, inject, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { TelemetriaService } from '../../services/telemetria.service';
import { TarifaEventosService } from '../../services/tarifa-eventos.service';

/**
 * Página "Laboratorio · Métricas" — estilo panel de observabilidad.
 * Mide latencias reales de las peticiones a los microservicios de Telemetría
 * y calcula percentiles (p50/p95/p99) + una serie temporal en vivo.
 */
@Component({
  selector: 'app-lab-metricas',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './lab-metricas.component.html',
  styleUrl: './lab-metricas.component.scss',
})
export class LabMetricasComponent implements OnDestroy {
  private telemetriaSvc = inject(TelemetriaService);
  private tarifaSvc = inject(TarifaEventosService);

  corriendo = false;
  muestras: number[] = [];        // latencias en ms
  serie: number[] = [];           // últimas N latencias para el gráfico
  readonly maxSerie = 40;
  objetivoP95 = 300;              // ms

  p50 = 0;
  p95 = 0;
  p99 = 0;
  ultima = 0;
  total = 0;
  private timer: any = null;

  ngOnDestroy(): void {
    this.detener();
  }

  iniciar(): void {
    if (this.corriendo) return;
    this.corriendo = true;
    this.tick();
    this.timer = setInterval(() => this.tick(), 900);
  }

  detener(): void {
    this.corriendo = false;
    if (this.timer) { clearInterval(this.timer); this.timer = null; }
  }

  limpiar(): void {
    this.muestras = []; this.serie = [];
    this.p50 = this.p95 = this.p99 = this.ultima = this.total = 0;
  }

  private async tick(): Promise<void> {
    const t0 = performance.now();
    try {
      // alterna entre dos endpoints reales para medir latencia de ida y vuelta
      if (this.total % 2 === 0) {
        await firstValueFrom(this.telemetriaSvc.listar());
      } else {
        await firstValueFrom(this.tarifaSvc.listar());
      }
    } catch {
      /* se cuenta igual la latencia hasta el fallo */
    }
    const ms = Math.round(performance.now() - t0);
    this.ultima = ms;
    this.total++;
    this.muestras.push(ms);
    if (this.muestras.length > 500) this.muestras.shift();
    this.serie.push(ms);
    if (this.serie.length > this.maxSerie) this.serie.shift();
    this.recalcular();
  }

  private recalcular(): void {
    const s = [...this.muestras].sort((a, b) => a - b);
    this.p50 = this.percentil(s, 50);
    this.p95 = this.percentil(s, 95);
    this.p99 = this.percentil(s, 99);
  }

  private percentil(sorted: number[], p: number): number {
    if (!sorted.length) return 0;
    const idx = Math.min(sorted.length - 1, Math.ceil((p / 100) * sorted.length) - 1);
    return sorted[Math.max(0, idx)];
  }

  /** ancho de la barra de un percentil (0..100%) relativo a un techo visual. */
  barra(valor: number): number {
    const techo = Math.max(this.objetivoP95 * 1.5, this.p99, 1);
    return Math.min(100, (valor / techo) * 100);
  }

  claseP95(): string {
    return this.p95 <= this.objetivoP95 ? 'ok' : 'warn';
  }

  /** puntos del sparkline (polyline) a partir de la serie. */
  get puntos(): string {
    if (!this.serie.length) return '';
    const w = 100, h = 40;
    const max = Math.max(...this.serie, this.objetivoP95, 1);
    const step = this.serie.length > 1 ? w / (this.serie.length - 1) : w;
    return this.serie
      .map((v, i) => `${(i * step).toFixed(2)},${(h - (v / max) * h).toFixed(2)}`)
      .join(' ');
  }

  get objetivoY(): number {
    const max = Math.max(...this.serie, this.objetivoP95, 1);
    return 40 - (this.objetivoP95 / max) * 40;
  }
}
