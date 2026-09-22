import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { gw } from '../core/gateway';

/**
 * Motor de pruebas de carga (experimentos) que se ejecuta EN EL NAVEGADOR.
 *
 * Dispara N peticiones HTTP contra un endpoint ya publicado en el gateway WSO2
 * remoto (Synology) — no corre nada en local ni toca el backend. Sirve para:
 *  - Medir latencia por request (min/max/avg/p95) y throughput (req/s).
 *  - Contar éxitos/errores agrupados por código HTTP.
 *  - Generar tráfico real observable en Kiali/Prometheus/Jaeger y disparar el
 *    HPA / circuit breaker del service mesh.
 *
 * La concurrencia es configurable: con concurrencia=1 el disparo es secuencial;
 * con concurrencia>1 se lanzan lotes en paralelo hasta completar N.
 */

/** Verbo HTTP soportado por una operación del catálogo. */
export type MetodoHttp = 'GET' | 'POST';

/** Operación blanco del experimento (endpoint concreto de un servicio). */
export interface OperacionCarga {
  /** Clave única para el <select>. */
  clave: string;
  /** Etiqueta legible. */
  etiqueta: string;
  /** Nombre del servicio publicado en el gateway (gateway.ts SERVICIOS). */
  servicio: string;
  /** Método HTTP. */
  metodo: MetodoHttp;
  /** Path relativo al servicio (se compone con gw()). */
  path: string;
  /** Cuerpo por defecto para operaciones POST (JSON). */
  bodyEjemplo?: unknown;
  /** true si la operación NO muta estado (segura para repetir masivamente). */
  segura: boolean;
  /** Descripción de qué demuestra. */
  nota?: string;
}

/** Resultado de una única petición del experimento. */
export interface ResultadoRequest {
  indice: number;
  ok: boolean;
  status: number;
  /** Latencia en milisegundos (redondeada). */
  ms: number;
  error?: string;
}

/** Estadísticas agregadas de una corrida completa. */
export interface EstadisticasCorrida {
  total: number;
  exitos: number;
  errores: number;
  exitoPct: number;
  min: number;
  max: number;
  avg: number;
  p95: number;
  p99: number;
  /** Duración total del experimento (ms). */
  duracionTotalMs: number;
  /** Throughput observado (requests por segundo). */
  reqPorSeg: number;
  /** Conteo de resultados agrupados por código HTTP (0 = error de red). */
  porCodigo: Record<number, number>;
}

/** Configuración de una corrida. */
export interface ConfigCorrida {
  operacion: OperacionCarga;
  numRequests: number;
  concurrencia: number;
  /** Body JSON (ya parseado) para operaciones POST; opcional. */
  body?: unknown;
}

@Injectable({ providedIn: 'root' })
export class ExperimentosService {
  private http = inject(HttpClient);

  /**
   * Catálogo de operaciones blanco. Se priorizan operaciones GET seguras
   * (health y listados) para no ensuciar el estado en memoria de los servicios
   * al repetir cientos de veces. Se incluye un POST de ejemplo por servicio
   * para quien quiera medir carga de escritura.
   */
  readonly catalogo: OperacionCarga[] = [
    {
      clave: 'tarifas-health',
      etiqueta: 'Tarifas · GET /actuator/health',
      servicio: 'tarifas',
      metodo: 'GET',
      path: 'actuator/health',
      segura: true,
      nota: 'Ping de salud. Ideal para medir latencia base del gateway + servicio.',
    },
    {
      clave: 'tarifas-listar',
      etiqueta: 'Tarifas · GET listar',
      servicio: 'tarifas',
      metodo: 'GET',
      path: 'api/v1/tarifas',
      segura: true,
      nota: 'Lectura del listado de tarifas (no muta estado).',
    },
    {
      clave: 'volumetria-health',
      etiqueta: 'Volumetría · GET /actuator/health',
      servicio: 'volumetria',
      metodo: 'GET',
      path: 'actuator/health',
      segura: true,
    },
    {
      clave: 'pagos-health',
      etiqueta: 'Pagos · GET /actuator/health',
      servicio: 'pagos',
      metodo: 'GET',
      path: 'actuator/health',
      segura: true,
    },
    {
      clave: 'facturacion-health',
      etiqueta: 'Facturación · GET /actuator/health',
      servicio: 'facturacion',
      metodo: 'GET',
      path: 'actuator/health',
      segura: true,
      nota: 'El circuit breaker más agresivo del mesh vive aquí; buen blanco para forzar 503.',
    },
    {
      clave: 'liquidacion-p2p-health',
      etiqueta: 'Liquidación P2P · GET /actuator/health',
      servicio: 'liquidacion-p2p',
      metodo: 'GET',
      path: 'actuator/health',
      segura: true,
    },
    {
      clave: 'liquidacion-mensual-health',
      etiqueta: 'Liquidación Mensual · GET /actuator/health',
      servicio: 'liquidacion-mensual',
      metodo: 'GET',
      path: 'actuator/health',
      segura: true,
    },
    {
      clave: 'volumetria-ingestar',
      etiqueta: 'Volumetría · POST ingestar lectura (escritura)',
      servicio: 'volumetria',
      metodo: 'POST',
      path: 'api/v1/lecturas',
      segura: false,
      bodyEjemplo: {
        medidorId: 'MED-LOAD-001',
        kwh: 12.5,
        direccion: 'CONSUMO',
        capturadaEn: '2026-01-01T10:00:00',
      },
      nota: 'Carga de escritura. Genera estado en memoria en el servicio.',
    },
    {
      clave: 'pagos-procesar',
      etiqueta: 'Pagos · POST procesar pago (escritura)',
      servicio: 'pagos',
      metodo: 'POST',
      path: 'api/v1/pagos',
      segura: false,
      bodyEjemplo: {
        prosumidorId: 'PRO-LOAD-001',
        referencia: 'FAC-LOAD',
        tipo: 'COBRO_FACTURA',
        monto: 1000,
      },
      nota: 'Carga de escritura sobre el servicio que mueve dinero (idempotencia por referencia).',
    },
    {
      clave: 'siniestros-health',
      etiqueta: 'Siniestros · GET /actuator/health',
      servicio: 'siniestros',
      metodo: 'GET' as MetodoHttp,
      path: 'actuator/health',
      segura: true,
      nota: 'Bounded context Siniestros (Entrega 3 EDA). Verifica disponibilidad.',
    },
    {
      clave: 'reaseguro-health',
      etiqueta: 'Reaseguro · GET /actuator/health',
      servicio: 'reaseguro',
      metodo: 'GET' as MetodoHttp,
      path: 'actuator/health',
      segura: true,
      nota: 'Bounded context Reaseguro (Entrega 3 EDA/CQRS). Verifica disponibilidad.',
    },
    {
      clave: 'siniestros-listar',
      etiqueta: 'Siniestros · GET listar',
      servicio: 'siniestros',
      metodo: 'GET' as MetodoHttp,
      path: 'api/v1/siniestros',
      segura: true,
      nota: 'Listado de siniestros (lectura).',
    },
    {
      clave: 'reaseguro-cesiones',
      etiqueta: 'Reaseguro · GET cesiones (lado lectura CQRS)',
      servicio: 'reaseguro',
      metodo: 'GET' as MetodoHttp,
      path: 'api/v1/cesiones',
      segura: true,
      nota: 'Vista de lectura CQRS: cesiones de riesgo proyectadas desde eventos.',
    },
    {
      clave: 'siniestros-reportar',
      etiqueta: 'Siniestros · POST reportar (escritura + evento)',
      servicio: 'siniestros',
      metodo: 'POST' as MetodoHttp,
      path: 'api/v1/siniestros',
      segura: false,
      bodyEjemplo: {
        polizaId: 'POL-EDA-001',
        prosumidorId: 'PRO-EDA-001',
        descripcion: 'Daño por sobretensión en panel solar',
        montoReclamacion: 2500000,
        fechaOcurrencia: '2026-09-14T10:00:00',
      },
      nota: 'Crea siniestro REPORTADO. Para emitir el evento, apruébelo después.',
    },
  ];

  /** Devuelve una operación por su clave. */
  operacion(clave: string): OperacionCarga | undefined {
    return this.catalogo.find((o) => o.clave === clave);
  }

  /**
   * Ejecuta una única petición y devuelve su resultado con latencia medida.
   * Nunca lanza: los errores HTTP o de red se capturan y se reportan como
   * resultado fallido (para no abortar el experimento completo).
   */
  private async dispararUna(cfg: ConfigCorrida, indice: number): Promise<ResultadoRequest> {
    const { operacion, body } = cfg;
    const url = gw(operacion.servicio, operacion.path);
    const inicio = performance.now();
    try {
      const obs =
        operacion.metodo === 'POST'
          ? this.http.post(url, body ?? operacion.bodyEjemplo ?? {}, {
              observe: 'response',
              responseType: 'text',
            })
          : this.http.get(url, { observe: 'response', responseType: 'text' });
      const resp = (await firstValueFrom(obs)) as HttpResponse<string>;
      const ms = Math.round(performance.now() - inicio);
      return { indice, ok: true, status: resp.status, ms };
    } catch (e) {
      const ms = Math.round(performance.now() - inicio);
      const status = e instanceof HttpErrorResponse ? e.status : 0;
      const error = e instanceof HttpErrorResponse ? e.message : String(e);
      return { indice, ok: status >= 200 && status < 400, status, ms, error };
    }
  }

  /**
   * Ejecuta el experimento completo: N peticiones con la concurrencia indicada.
   * Invoca `onProgress` tras cada request terminada para poder mostrar avance
   * y resultados en vivo. Devuelve la lista completa de resultados.
   */
  async ejecutar(
    cfg: ConfigCorrida,
    onProgress?: (r: ResultadoRequest, completadas: number) => void,
  ): Promise<ResultadoRequest[]> {
    const total = Math.max(1, Math.floor(cfg.numRequests));
    const concurrencia = Math.min(Math.max(1, Math.floor(cfg.concurrencia)), total);
    const resultados: ResultadoRequest[] = new Array(total);
    let siguiente = 0;
    let completadas = 0;

    // Worker: toma el siguiente índice disponible hasta agotar la cola.
    const worker = async () => {
      while (true) {
        const i = siguiente++;
        if (i >= total) break;
        const r = await this.dispararUna(cfg, i);
        resultados[i] = r;
        completadas++;
        onProgress?.(r, completadas);
      }
    };

    const workers = Array.from({ length: concurrencia }, () => worker());
    await Promise.all(workers);
    return resultados;
  }

  /** Calcula estadísticas agregadas a partir de los resultados y la duración. */
  calcularEstadisticas(resultados: ResultadoRequest[], duracionTotalMs: number): EstadisticasCorrida {
    const total = resultados.length;
    const exitos = resultados.filter((r) => r.ok).length;
    const errores = total - exitos;
    const latencias = resultados.map((r) => r.ms).sort((a, b) => a - b);

    const porCodigo: Record<number, number> = {};
    for (const r of resultados) {
      porCodigo[r.status] = (porCodigo[r.status] ?? 0) + 1;
    }

    const percentil = (p: number): number => {
      if (latencias.length === 0) return 0;
      const rank = Math.ceil((p / 100) * latencias.length) - 1;
      const idx = Math.min(Math.max(rank, 0), latencias.length - 1);
      return latencias[idx];
    };

    const suma = latencias.reduce((a, b) => a + b, 0);
    const avg = total ? Math.round(suma / total) : 0;
    const dur = Math.max(duracionTotalMs, 1);

    return {
      total,
      exitos,
      errores,
      exitoPct: total ? Math.round((exitos / total) * 1000) / 10 : 0,
      min: latencias[0] ?? 0,
      max: latencias[latencias.length - 1] ?? 0,
      avg,
      p95: percentil(95),
      p99: percentil(99),
      duracionTotalMs: Math.round(duracionTotalMs),
      reqPorSeg: Math.round((total / dur) * 1000 * 10) / 10,
      porCodigo,
    };
  }
}
