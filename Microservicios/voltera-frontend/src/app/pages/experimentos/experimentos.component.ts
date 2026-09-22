import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import {
  ConfigCorrida,
  EstadisticasCorrida,
  ExperimentosService,
  OperacionCarga,
  ResultadoRequest,
} from '../../services/experimentos.service';
import { SiniestrosService } from '../../services/siniestros.service';
import { ReaseguroService } from '../../services/reaseguro.service';
import { ToastService } from '../../core/toast.service';
import { SiniestroAprobadoEvento } from '../../core/models';

/** Resultado de un experimento de validación EDA. */
interface ResultadoExperimento {
  ok: boolean;
  detalle: string;
}

/** Definición de una tarjeta de experimento de validación. */
interface ExperimentoValidacion {
  num: number;
  nombre: string;
  hipotesis: string;
  metrica: string;
  criterio: string;
  ejecutando: boolean;
  resultado?: ResultadoExperimento;
  ejecutar: () => Promise<ResultadoExperimento>;
}

/**
 * Página "Experimentos de arquitectura".
 *
 * Dos secciones:
 *  A) Motor de carga: N requests, concurrencia y métricas (latencia, throughput).
 *  B) 4 experimentos de validación de la Entrega 3 (EDA, KEDA, CQRS, idempotencia)
 *     que ejecutan secuencias reales de llamadas HTTP contra siniestros-service y
 *     reaseguro-service a través del gateway WSO2 remoto.
 *
 * No corre nada en local ni modifica el backend: solo consume endpoints ya
 * publicados a través del gateway.
 */
@Component({
  selector: 'app-experimentos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './experimentos.component.html',
  styleUrl: './domain-page.scss',
})
export class ExperimentosComponent {
  private svc = inject(ExperimentosService);
  private siniestrosSvc = inject(SiniestrosService);
  private reaseguroSvc = inject(ReaseguroService);
  private toast = inject(ToastService);

  activeTab: 'carga' | 'validacion' = 'carga';

  readonly catalogo: OperacionCarga[] = this.svc.catalogo;

  // ----- Motor de carga -----
  operacionClave = this.catalogo[0].clave;
  numRequests = 20;
  concurrencia = 5;
  bodyTexto = '';
  readonly presets = [10, 20, 50, 100, 200];

  ejecutando = false;
  completadas = 0;
  totalPlan = 0;
  resultados: ResultadoRequest[] = [];
  stats?: EstadisticasCorrida;
  iniciadoEn?: Date;

  // ----- Experimentos de validación EDA -----
  experimentos: ExperimentoValidacion[] = [
    {
      num: 1,
      nombre: 'EDA · Los eventos no se pierden',
      hipotesis:
        'Si el consumidor (reaseguro-service) está caído cuando se aprueba un siniestro, el evento queda en el broker y se procesa al volver.',
      metrica: 'Número de cesiones creadas en reaseguro vs. siniestros aprobados en siniestros-service.',
      criterio: '100% de los eventos aprobados generan cesión (eventual).',
      ejecutando: false,
      ejecutar: () => this.expEventosNoSePierden(),
    },
    {
      num: 2,
      nombre: 'KEDA · Escalado por lag',
      hipotesis:
        'Cuando se inyecta un pico de eventos (≥50 siniestros aprobados en pocos segundos), KEDA detecta el lag y escala las réplicas de reaseguro-service. Con cola vacía, escala a cero.',
      metrica: 'Número de réplicas de reaseguro-service antes y después del pico (observable en Kiali/Prometheus).',
      criterio: 'Réplicas > 1 durante el pico; réplicas = 0 en reposo.',
      ejecutando: false,
      ejecutar: () => this.expKedaEscalado(),
    },
    {
      num: 3,
      nombre: 'CQRS · Retraso escritura → lectura',
      hipotesis:
        'La vista de lectura (cesiones) se actualiza en menos de 2 segundos tras la escritura (aprobación del siniestro).',
      metrica: 'Tiempo entre POST /aprobar y la aparición de la cesión en GET /cesiones.',
      criterio: 'Retraso < 2000 ms.',
      ejecutando: false,
      ejecutar: () => this.expCqrsRetraso(),
    },
    {
      num: 4,
      nombre: 'Transferencia de estado + Idempotencia',
      hipotesis:
        'Al simular el reenvío del mismo evento (mismo eventId) a reaseguro-service, no se crea un duplicado.',
      metrica: 'Cantidad de cesiones con el mismo siniestroId antes y después del reenvío.',
      criterio: 'Cantidad no cambia (0 duplicados).',
      ejecutando: false,
      ejecutar: () => this.expIdempotencia(),
    },
  ];

  // =============== Motor de carga ===============

  get operacion(): OperacionCarga {
    return this.svc.operacion(this.operacionClave) ?? this.catalogo[0];
  }

  get progresoPct(): number {
    return this.totalPlan ? Math.round((this.completadas / this.totalPlan) * 100) : 0;
  }

  get codigos(): { codigo: number; conteo: number }[] {
    if (!this.stats) return [];
    return Object.entries(this.stats.porCodigo)
      .map(([c, n]) => ({ codigo: Number(c), conteo: n }))
      .sort((a, b) => a.codigo - b.codigo);
  }

  alCambiarOperacion(): void {
    const op = this.operacion;
    this.bodyTexto = op.metodo === 'POST' && op.bodyEjemplo ? JSON.stringify(op.bodyEjemplo, null, 2) : '';
  }

  setPreset(n: number): void {
    this.numRequests = n;
  }

  async lanzar(): Promise<void> {
    if (this.ejecutando) return;
    const op = this.operacion;

    let body: unknown;
    if (op.metodo === 'POST') {
      try {
        body = this.bodyTexto.trim() ? JSON.parse(this.bodyTexto) : op.bodyEjemplo;
      } catch {
        this.toast.error('JSON inválido', 'Revisa el cuerpo de la petición (debe ser JSON válido).');
        return;
      }
      if (!op.segura) {
        const ok = window.confirm(
          `Esta operación (${op.etiqueta}) ESCRIBE estado en el servicio y se ejecutará ${this.numRequests} veces. ¿Continuar?`,
        );
        if (!ok) return;
      }
    }

    const cfg: ConfigCorrida = {
      operacion: op,
      numRequests: this.numRequests,
      concurrencia: this.concurrencia,
      body,
    };

    this.ejecutando = true;
    this.completadas = 0;
    this.totalPlan = this.numRequests;
    this.resultados = [];
    this.stats = undefined;
    this.iniciadoEn = new Date();

    const t0 = performance.now();
    try {
      const resultados = await this.svc.ejecutar(cfg, (_r, completadas) => {
        this.completadas = completadas;
      });
      const dur = performance.now() - t0;
      this.resultados = resultados;
      this.stats = this.svc.calcularEstadisticas(resultados, dur);
      const s = this.stats;
      if (s.errores === 0) {
        this.toast.success(
          'Experimento completado',
          `${s.total} req · ${s.exitoPct}% éxito · avg ${s.avg} ms · ${s.reqPorSeg} req/s`,
        );
      } else {
        this.toast.error(
          'Completado con errores',
          `${s.errores}/${s.total} fallaron · éxito ${s.exitoPct}%`,
        );
      }
    } catch (e) {
      this.toast.error('Error al ejecutar el experimento', String(e));
    } finally {
      this.ejecutando = false;
    }
  }

  limpiar(): void {
    this.resultados = [];
    this.stats = undefined;
    this.completadas = 0;
    this.totalPlan = 0;
    this.iniciadoEn = undefined;
  }

  exportarCsv(): void {
    if (!this.resultados.length) return;
    const cab = 'indice,ok,status,ms,error\n';
    const filas = this.resultados
      .map((r) => `${r.indice},${r.ok},${r.status},${r.ms},"${(r.error ?? '').replace(/"/g, "'")}"`)
      .join('\n');
    const blob = new Blob([cab + filas], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    const ts = new Date().toISOString().replace(/[:.]/g, '-');
    a.href = url;
    a.download = `experimento-${this.operacion.clave}-${ts}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }

  claseBadge(codigo: number): string {
    if (codigo >= 200 && codigo < 300) return 'ok';
    if (codigo >= 500 || codigo === 0) return 'err';
    return 'warn';
  }

  claseLatencia(ms: number): string {
    if (ms < 200) return 'ok';
    if (ms < 800) return 'warn';
    return 'err';
  }

  // =============== Experimentos de validación ===============

  /** Ejecuta un experimento y actualiza su estado/resultado. */
  async ejecutarExperimento(exp: ExperimentoValidacion): Promise<void> {
    if (exp.ejecutando) return;
    exp.ejecutando = true;
    exp.resultado = undefined;
    try {
      exp.resultado = await exp.ejecutar();
      if (exp.resultado.ok) {
        this.toast.success(`Exp ${exp.num} · ÉXITO`, exp.resultado.detalle);
      } else {
        this.toast.error(`Exp ${exp.num} · FALLO`, exp.resultado.detalle);
      }
    } catch (e) {
      exp.resultado = { ok: false, detalle: `Error inesperado: ${String(e)}` };
      this.toast.error(`Exp ${exp.num} · Error`, String(e));
    } finally {
      exp.ejecutando = false;
    }
  }

  private demora(ms: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }

  /** Exp 1 — EDA: reporta y aprueba N siniestros, luego cuenta cesiones. */
  private async expEventosNoSePierden(): Promise<ResultadoExperimento> {
    const N = 3;
    const poliza = `POL-EDA-E1-${Date.now()}`;
    let aprobados = 0;

    for (let i = 0; i < N; i++) {
      const s = await firstValueFrom(
        this.siniestrosSvc.reportar({
          polizaId: poliza,
          prosumidorId: `PRO-EDA-E1-${i}`,
          descripcion: `Siniestro EDA E1 #${i}`,
          montoReclamacion: 1000000 + i * 100000,
          fechaOcurrencia: '2026-09-14T10:00:00',
        }),
      );
      await firstValueFrom(this.siniestrosSvc.aprobar(s.id));
      aprobados++;
    }

    // Espera a que el consumidor procese los eventos (eventual consistency).
    await this.demora(2000);
    const cesiones = await firstValueFrom(this.reaseguroSvc.porPoliza(poliza));
    const encontradas = cesiones.length;
    const ok = encontradas >= aprobados;
    return {
      ok,
      detalle: `Aprobados: ${aprobados} · Cesiones encontradas para ${poliza}: ${encontradas}. ${
        ok ? '100% de los eventos generaron cesión.' : 'Faltan cesiones (el consumidor puede seguir procesando; reintente).'
      }`,
    };
  }

  /** Exp 2 — KEDA: inyecta un pico de ≥50 escrituras usando el motor de carga. */
  private async expKedaEscalado(): Promise<ResultadoExperimento> {
    const op = this.svc.operacion('siniestros-reportar');
    if (!op) return { ok: false, detalle: 'No se encontró la operación siniestros-reportar en el catálogo.' };

    const N = 50;
    const cfg: ConfigCorrida = {
      operacion: op,
      numRequests: N,
      concurrencia: 10,
      body: op.bodyEjemplo,
    };
    const t0 = performance.now();
    const resultados = await this.svc.ejecutar(cfg);
    const dur = performance.now() - t0;
    const s = this.svc.calcularEstadisticas(resultados, dur);
    const ok = s.errores === 0;
    return {
      ok,
      detalle: `Pico inyectado: ${s.total} siniestros en ${Math.round(s.duracionTotalMs)} ms (${s.reqPorSeg} req/s, ${s.exitoPct}% éxito). El escalado de réplicas (KEDA por lag de la cola) es observable en Prometheus/Kiali — no se puede consultar Kubernetes desde el navegador. Verifica las réplicas de reaseguro-service allí: >1 durante el pico, 0 en reposo.`,
    };
  }

  /** Exp 3 — CQRS: mide el retraso entre aprobar y ver la cesión en lectura. */
  private async expCqrsRetraso(): Promise<ResultadoExperimento> {
    const poliza = `POL-EDA-E3-${Date.now()}`;
    const s = await firstValueFrom(
      this.siniestrosSvc.reportar({
        polizaId: poliza,
        prosumidorId: 'PRO-EDA-E3',
        descripcion: 'Siniestro EDA E3 (medición CQRS)',
        montoReclamacion: 3000000,
        fechaOcurrencia: '2026-09-14T10:00:00',
      }),
    );
    const tAprobar = performance.now();
    await firstValueFrom(this.siniestrosSvc.aprobar(s.id));

    // Poll cada 200ms hasta que aparezca la cesión (máx 10s).
    const maxMs = 10000;
    let delta = -1;
    while (performance.now() - tAprobar < maxMs) {
      const cesiones = await firstValueFrom(this.reaseguroSvc.porPoliza(poliza));
      if (cesiones.length > 0) {
        delta = Math.round(performance.now() - tAprobar);
        break;
      }
      await this.demora(200);
    }

    if (delta < 0) {
      return { ok: false, detalle: `La cesión no apareció en la vista de lectura tras ${maxMs} ms.` };
    }
    const ok = delta < 2000;
    return {
      ok,
      detalle: `Retraso escritura→lectura: ${delta} ms. Criterio < 2000 ms → ${ok ? 'CUMPLE' : 'NO CUMPLE'}.`,
    };
  }

  /** Exp 4 — Idempotencia: reenvía el mismo eventId y verifica que no duplica. */
  private async expIdempotencia(): Promise<ResultadoExperimento> {
    const ahora = new Date().toISOString().slice(0, 19);
    const eventId = `EVT-IDEMP-${Date.now()}`;
    const siniestroId = `SIN-IDEMP-${Date.now()}`;
    const poliza = `POL-EDA-E4-${Date.now()}`;
    const evento: SiniestroAprobadoEvento = {
      eventId,
      siniestroId,
      polizaId: poliza,
      prosumidorId: 'PRO-EDA-E4',
      montoAprobado: 2000000,
      descripcion: 'Evento EDA E4 (prueba idempotencia)',
      aprobadoEn: ahora,
      emitidoEn: ahora,
    };

    // Primer envío.
    await firstValueFrom(this.reaseguroSvc.simularEvento(evento));
    await this.demora(300);
    const antes = (await firstValueFrom(this.reaseguroSvc.porPoliza(poliza))).filter(
      (c) => c.siniestroId === siniestroId,
    ).length;

    // Reenvío con el MISMO eventId.
    await firstValueFrom(this.reaseguroSvc.simularEvento(evento));
    await this.demora(300);
    const despues = (await firstValueFrom(this.reaseguroSvc.porPoliza(poliza))).filter(
      (c) => c.siniestroId === siniestroId,
    ).length;

    const ok = despues === antes && antes >= 1;
    return {
      ok,
      detalle: `Cesiones para siniestroId antes del reenvío: ${antes} · después: ${despues}. ${
        ok ? '0 duplicados — idempotencia correcta.' : 'Se detectó cambio en el conteo (posible duplicado).'
      }`,
    };
  }
}
