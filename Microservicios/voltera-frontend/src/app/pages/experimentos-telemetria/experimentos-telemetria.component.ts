import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { TelemetriaService } from '../../services/telemetria.service';
import { TarifaEventosService } from '../../services/tarifa-eventos.service';
import { ReaseguroService } from '../../services/reaseguro.service';
import { SiniestrosService } from '../../services/siniestros.service';
import { ToastService } from '../../core/toast.service';
import { describeHttpError } from '../../core/http-error';
import { SimularConsumoEvento } from '../../core/models';
import { PatronesComponent } from '../patrones/patrones.component';

interface ResultadoExperimento {
  ok: boolean;
  detalle: string;
  pasos?: string[];
}

/** Fase de un evento en el timeline: emitido al broker o proyectado (CQRS lectura). */
export type FaseEvento = 'emitido' | 'consumido' | 'proyectado';

/** Un evento del flujo EDA mostrado en el timeline global. */
export interface EventoEda {
  ts: string;
  tipo: string;
  servicio: string;
  detalle: string;
  fase: FaseEvento;
  ref?: string;
}

/** Etapas del flujo EDA que se iluminan en el diagrama. */
type EtapaFlujo = 'medidor' | 'broker' | 'consumidor' | 'cargo';
type EstadoEtapa = 'idle' | 'activo' | 'hecho';

interface ExperimentoTelemetria {
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
 * Página "Experimentos de Telemetría (EDA)".
 *
 * Ejecuta secuencias reales de HTTP contra telemetria-service (8093) y
 * tarifa-eventos-service (8094) a través del gateway, para demostrar en vivo:
 *  1. EDA end-to-end: consumo IoT → evento → broker → cargo (CQRS lectura).
 *  2. CQRS: retraso escritura → lectura.
 *  3. Idempotencia + transferencia de estado: reenviar el mismo eventId no duplica.
 *  4. Notificación liquidada: activar un medidor genera factura/tarifas.
 */
@Component({
  selector: 'app-experimentos-telemetria',
  standalone: true,
  imports: [CommonModule, FormsModule, PatronesComponent],
  templateUrl: './experimentos-telemetria.component.html',
  styleUrl: './domain-page.scss',
})
export class ExperimentosTelemetriaComponent {
  private telemetriaSvc = inject(TelemetriaService);
  private tarifaSvc = inject(TarifaEventosService);
  private reaseguroSvc = inject(ReaseguroService);
  private siniestrosSvc = inject(SiniestrosService);
  private toast = inject(ToastService);

  /** Pestaña principal: ejecutar experimentos o ver patrones/tecnología. */
  vista: 'experimentos' | 'patrones' = 'experimentos';

  /** Estado del flujo REAL disparado desde el botón "Emitir evento" de Patrones. */
  flujoReal: { activo: boolean; texto: string; ok?: boolean } = {
    activo: false,
    texto: 'Pulsa "▶ Emitir evento" para ejecutar el flujo real de Telemetría contra los servicios.',
  };

  /**
   * Ejecuta el flujo REAL de Telemetría (el experimento que se presenta):
   * registra + activa un medidor, ingesta consumo extra (emite ConsumoRegistrado),
   * y espera a que tarifa-eventos cree el cargo por el broker. Alimenta el banner
   * de la sección Patrones para que el botón quede asociado al experimento real.
   */
  async emitirEventoReal(): Promise<void> {
    if (this.flujoReal.activo) return;
    this.flujoReal = { activo: true, texto: '📡 Ingestando consumo del medidor IoT…', ok: undefined };
    try {
      const umbral = 100;
      const consumo = 150;
      const esperado = (consumo - umbral) * 850;
      const medidor = await firstValueFrom(
        this.telemetriaSvc.registrar({ prosumidorId: `PRO-EMIT-${Date.now()}`, umbralKwh: umbral }),
      );
      await firstValueFrom(this.telemetriaSvc.activar(medidor.id));
      this.flujoReal = { activo: true, texto: `⚡ Publicando ConsumoRegistrado (${consumo} kWh) al broker…` };
      const ingesta = await firstValueFrom(this.telemetriaSvc.ingestar(medidor.id, { consumoKwh: consumo }));
      this.evento(
        'ConsumoRegistrado',
        'telemetria-service',
        `${medidor.prosumidorId} · ${consumo} kWh · extra=${ingesta.consumoExtra} · broker=${ingesta.publicadoEnBroker}`,
        'emitido',
        `evt:${ingesta.eventId}`,
      );
      this.flujoReal = { activo: true, texto: `💰 tarifa-eventos consumiendo el evento (CQRS + idempotencia)…` };

      let cargo: any = null;
      for (let i = 0; i < 15; i++) {
        await new Promise((r) => setTimeout(r, 400));
        const cargos = (await firstValueFrom(this.tarifaSvc.listar())).filter((c) => c.medidorId === medidor.id);
        if (cargos.length) { cargo = cargos[0]; break; }
      }
      if (cargo) {
        const ok = cargo.estado === 'LIQUIDADO' && Math.round(cargo.montoCargo) === esperado;
        this.evento(
          'CargoGenerado',
          'tarifa-eventos-service',
          `${cargo.prosumidorId} · excedente ${cargo.excedenteKwh} kWh · ${cargo.montoCargo} COP · ${cargo.estado}`,
          'proyectado',
          `cargo:${cargo.id}`,
        );
        this.flujoReal = {
          activo: false,
          ok,
          texto: `🧾 Cargo ${cargo.estado} de ${cargo.montoCargo} COP (excedente ${cargo.excedenteKwh} kWh). Flujo EDA real completado: IoT → broker → tarifa (CQRS).`,
        };
        this.toast.success('Evento real procesado', `Cargo ${cargo.montoCargo} COP · ${cargo.estado}`);
      } else {
        this.flujoReal = { activo: false, ok: false, texto: 'El cargo no apareció aún; reintenta en unos segundos.' };
      }
    } catch (e) {
      this.flujoReal = { activo: false, ok: false, texto: `Error: ${describeHttpError(e)}` };
      this.toast.error('Error en el flujo real', describeHttpError(e));
    }
  }

  /** Estado de cada etapa del diagrama de flujo (para la animación). */
  flujo: Record<EtapaFlujo, EstadoEtapa> = {
    medidor: 'idle',
    broker: 'idle',
    consumidor: 'idle',
    cargo: 'idle',
  };

  /** Datos vivos que se muestran bajo cada nodo del diagrama. */
  flujoDatos: Record<EtapaFlujo, string> = {
    medidor: '',
    broker: '',
    consumidor: '',
    cargo: '',
  };

  /** Log en vivo del experimento en curso (se ve el flujo paso a paso). */
  logVivo: string[] = [];
  corriendoAlguno = false;

  /**
   * Timeline GLOBAL de eventos EDA. A diferencia de `logVivo` (que se resetea en
   * cada experimento), este acumula TODOS los eventos que van ocurriendo a partir
   * de los experimentos: eventos emitidos (ConsumoRegistrado, SiniestroAprobado,
   * NotificacionLiquidada) y eventos proyectados/consumidos (Cargo, Cesion).
   */
  eventos: EventoEda[] = [];
  cargandoEventos = false;

  /** Registra un evento en el timeline global (lo más reciente primero). */
  private evento(
    tipo: string,
    servicio: string,
    detalle: string,
    fase: FaseEvento = 'emitido',
    ref?: string,
  ): void {
    const ev: EventoEda = {
      ts: new Date().toISOString(),
      tipo,
      servicio,
      detalle,
      fase,
      ref,
    };
    // Evita duplicados exactos por ref (p. ej. al refrescar proyecciones).
    if (ref && this.eventos.some((e) => e.ref === ref && e.tipo === tipo)) return;
    this.eventos = [ev, ...this.eventos];
  }

  /** Limpia el timeline de eventos. */
  limpiarEventos(): void {
    this.eventos = [];
  }

  /**
   * Trae TODOS los eventos ya proyectados por el backend (lado lectura CQRS):
   * cargos de tarifa-eventos y cesiones de reaseguro, y los vuelca al timeline.
   * Permite "ver todos los eventos" aunque se hayan generado en sesiones previas
   * o por el load-generator del clúster.
   */
  async cargarTodosLosEventos(): Promise<void> {
    if (this.cargandoEventos) return;
    this.cargandoEventos = true;
    try {
      const [cargos, cesiones] = await Promise.all([
        firstValueFrom(this.tarifaSvc.listar()).catch(() => []),
        firstValueFrom(this.reaseguroSvc.listar()).catch(() => []),
      ]);
      for (const c of cargos) {
        this.evento(
          'CargoGenerado',
          'tarifa-eventos-service',
          `${c.prosumidorId} · excedente ${c.excedenteKwh} kWh · ${c.montoCargo} COP · ${c.estado}`,
          'proyectado',
          `cargo:${c.id}`,
        );
      }
      for (const ce of cesiones) {
        this.evento(
          'CesionCreada',
          'reaseguro-service',
          `${ce.prosumidorId} · cedido ${ce.montoCedido} de ${ce.montoAprobado} COP (${ce.porcentajeCedido}%) · ${ce.estado}`,
          'proyectado',
          `cesion:${ce.id}`,
        );
      }
      this.toast.success('Eventos cargados', `${cargos.length} cargos · ${cesiones.length} cesiones`);
    } catch (e) {
      this.toast.error('No se pudieron cargar los eventos', describeHttpError(e));
    } finally {
      this.cargandoEventos = false;
    }
  }

  private resetFlujo(): void {
    this.flujo = { medidor: 'idle', broker: 'idle', consumidor: 'idle', cargo: 'idle' };
    this.flujoDatos = { medidor: '', broker: '', consumidor: '', cargo: '' };
    this.logVivo = [];
  }

  private marcar(etapa: EtapaFlujo, estado: EstadoEtapa, dato?: string): void {
    this.flujo[etapa] = estado;
    if (dato !== undefined) this.flujoDatos[etapa] = dato;
  }

  private log(linea: string): void {
    this.logVivo = [...this.logVivo, linea];
  }

  experimentos: ExperimentoTelemetria[] = [
    {
      num: 1,
      nombre: 'EDA end-to-end · Consumo IoT → cargo por evento',
      hipotesis:
        'Al ingestar una lectura de consumo extra, telemetria-service publica ConsumoRegistrado al broker; tarifa-eventos-service lo consume y crea el cargo. El emisor NO llama al consumidor.',
      metrica: 'El cargo aparece en la vista de lectura de tarifa-eventos con el monto = (consumo − umbral) × 850.',
      criterio: 'Aparece 1 cargo LIQUIDADO con el monto esperado (eventual, por el broker).',
      ejecutando: false,
      ejecutar: () => this.expEdaEndToEnd(),
    },
    {
      num: 2,
      nombre: 'CQRS · Retraso escritura → lectura',
      hipotesis:
        'La vista de lectura (cargos) se actualiza en menos de 3 s tras la ingesta (el evento viaja por el broker).',
      metrica: 'Tiempo entre la ingesta de la lectura y la aparición del cargo en GET /cargos.',
      criterio: 'Retraso < 3000 ms.',
      ejecutando: false,
      ejecutar: () => this.expCqrsRetraso(),
    },
    {
      num: 3,
      nombre: 'Idempotencia + transferencia de estado',
      hipotesis:
        'Reenviar el mismo evento (mismo eventId) a tarifa-eventos no crea un cargo duplicado. Los datos viajan en el evento (transferencia de estado).',
      metrica: 'Cantidad de cargos con el mismo prosumidorId antes y después del reenvío.',
      criterio: 'La cantidad no cambia (0 duplicados).',
      ejecutando: false,
      ejecutar: () => this.expIdempotencia(),
    },
    {
      num: 4,
      nombre: 'Notificación liquidada al activar medidor',
      hipotesis:
        'Al activar un nuevo medidor/prosumidor, tarifa-eventos genera y publica una notificación con factura y tarifas (cargo fijo, precio base, precio extra).',
      metrica: 'La notificación devuelta contiene cargo fijo, precio base y precio extra.',
      criterio: 'Se genera la notificación con tipo MEDIDOR_ACTIVADO y sus tarifas.',
      ejecutando: false,
      ejecutar: () => this.expNotificacion(),
    },
  ];

  async ejecutarExperimento(exp: ExperimentoTelemetria): Promise<void> {
    if (exp.ejecutando) return;
    exp.ejecutando = true;
    exp.resultado = undefined;
    this.corriendoAlguno = true;
    this.resetFlujo();
    try {
      exp.resultado = await exp.ejecutar();
      if (exp.resultado.ok) {
        this.toast.success(`Exp ${exp.num} · ÉXITO`, exp.resultado.detalle);
      } else {
        this.toast.error(`Exp ${exp.num} · FALLO`, exp.resultado.detalle);
      }
    } catch (e) {
      exp.resultado = { ok: false, detalle: `Error inesperado: ${describeHttpError(e)}` };
      this.toast.error(`Exp ${exp.num} · Error`, describeHttpError(e));
    } finally {
      exp.ejecutando = false;
      this.corriendoAlguno = false;
    }
  }

  async ejecutarTodos(): Promise<void> {
    for (const exp of this.experimentos) {
      await this.ejecutarExperimento(exp);
    }
  }

  private demora(ms: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }

  /** Exp 1 — EDA end-to-end: registrar + activar medidor, ingestar consumo extra, ver cargo. */
  private async expEdaEndToEnd(): Promise<ResultadoExperimento> {
    const pasos: string[] = [];
    const umbral = 100;
    const consumo = 150;
    const esperado = (consumo - umbral) * 850; // 42 500

    // --- Etapa 1: MEDIDOR IoT ---
    this.marcar('medidor', 'activo', 'registrando…');
    this.log('📡 Registrando medidor IoT…');
    const medidor = await firstValueFrom(
      this.telemetriaSvc.registrar({ prosumidorId: `PRO-EXP1-${Date.now()}`, umbralKwh: umbral }),
    );
    pasos.push(`1. Medidor ${medidor.id} (umbral ${umbral} kWh) · ${medidor.estado}`);
    this.log(`📡 Medidor ${medidor.id.slice(0, 16)}… registrado (INACTIVO)`);

    const activo = await firstValueFrom(this.telemetriaSvc.activar(medidor.id));
    pasos.push(`2. Medidor activado → ${activo.estado}`);
    this.log(`📡 Medidor ACTIVO`);

    this.log(`📡 Ingestando lectura de ${consumo} kWh (umbral ${umbral})…`);
    const ingesta = await firstValueFrom(this.telemetriaSvc.ingestar(medidor.id, { consumoKwh: consumo }));
    this.marcar('medidor', 'hecho', `${consumo} kWh · extra: ${ingesta.consumoExtra ? 'SÍ' : 'no'}`);
    pasos.push(`3. Lectura ${consumo} kWh · consumoExtra=${ingesta.consumoExtra} · publicadoEnBroker=${ingesta.publicadoEnBroker}`);
    this.evento(
      'ConsumoRegistrado',
      'telemetria-service',
      `${medidor.prosumidorId} · ${consumo} kWh · extra=${ingesta.consumoExtra} · broker=${ingesta.publicadoEnBroker}`,
      'emitido',
      `evt:${ingesta.eventId}`,
    );

    // --- Etapa 2: BROKER ---
    this.marcar('broker', 'activo', 'evento ConsumoRegistrado');
    this.log(`⚡ Evento ConsumoRegistrado publicado al broker (eventId ${ingesta.eventId.slice(0, 8)}…)`);
    await this.demora(500);
    this.marcar('broker', 'hecho', 'topic: consumo-registrado');

    // --- Etapa 3: CONSUMIDOR ---
    this.marcar('consumidor', 'activo', 'consumiendo…');
    this.log(`💰 tarifa-eventos-service consumiendo el evento (CQRS + idempotencia)…`);

    let cargos = [] as Awaited<ReturnType<typeof this.buscarCargos>>;
    for (let i = 0; i < 15; i++) {
      await this.demora(400);
      cargos = await this.buscarCargos(medidor.id);
      if (cargos.length > 0) break;
    }
    pasos.push(`4. Cargos encontrados para el medidor: ${cargos.length}`);

    if (cargos.length === 0) {
      this.marcar('consumidor', 'idle');
      this.log('⚠️ El cargo no apareció aún (reintente).');
      return { ok: false, detalle: 'El cargo no apareció (el consumidor puede seguir procesando; reintente).', pasos };
    }
    const cargo = cargos[0];
    this.marcar('consumidor', 'hecho', 'cargo calculado');
    this.log(`💰 Cargo calculado por transferencia de estado`);
    this.evento(
      'CargoGenerado',
      'tarifa-eventos-service',
      `${cargo.prosumidorId} · excedente ${cargo.excedenteKwh} kWh · ${cargo.montoCargo} COP · ${cargo.estado}`,
      'proyectado',
      `cargo:${cargo.id}`,
    );

    // --- Etapa 4: CARGO (lado lectura CQRS) ---
    this.marcar('cargo', 'hecho', `${cargo.montoCargo} COP · ${cargo.estado}`);
    this.log(`🧾 Cargo ${cargo.id.slice(0, 12)}… · ${cargo.estado} · excedente ${cargo.excedenteKwh} kWh · ${cargo.montoCargo} COP`);
    pasos.push(`5. Cargo ${cargo.id} · ${cargo.estado} · excedente ${cargo.excedenteKwh} kWh · ${cargo.montoCargo} COP`);

    const ok = cargo.estado === 'LIQUIDADO' && Math.round(cargo.montoCargo) === esperado;
    this.log(ok ? `✅ Flujo completo verificado (${cargo.montoCargo} COP = esperado ${esperado}).` : `❌ Monto ${cargo.montoCargo} ≠ esperado ${esperado}.`);
    return {
      ok,
      detalle: ok
        ? `Flujo EDA completo: consumo IoT → evento → broker → cargo LIQUIDADO de ${cargo.montoCargo} COP (esperado ${esperado}).`
        : `Cargo creado pero no coincide: estado ${cargo.estado}, monto ${cargo.montoCargo} (esperado ${esperado}).`,
      pasos,
    };
  }

  /** Exp 2 — CQRS: mide retraso ingesta → aparición del cargo. */
  private async expCqrsRetraso(): Promise<ResultadoExperimento> {
    const pasos: string[] = [];
    const medidor = await firstValueFrom(
      this.telemetriaSvc.registrar({ prosumidorId: `PRO-EXP2-${Date.now()}`, umbralKwh: 100 }),
    );
    await firstValueFrom(this.telemetriaSvc.activar(medidor.id));
    pasos.push(`Medidor ${medidor.id} activado.`);

    const t0 = performance.now();
    await firstValueFrom(this.telemetriaSvc.ingestar(medidor.id, { consumoKwh: 160 }));
    pasos.push('Lectura 160 kWh ingestada; midiendo hasta ver el cargo…');

    let delta = -1;
    while (performance.now() - t0 < 12000) {
      const cargos = await this.buscarCargos(medidor.id);
      if (cargos.length > 0) {
        delta = Math.round(performance.now() - t0);
        break;
      }
      await this.demora(200);
    }
    if (delta < 0) return { ok: false, detalle: 'El cargo no apareció en 12 s.', pasos };
    pasos.push(`Retraso escritura→lectura: ${delta} ms.`);
    const ok = delta < 3000;
    return { ok, detalle: `Retraso ${delta} ms · criterio < 3000 ms → ${ok ? 'CUMPLE' : 'NO CUMPLE'}.`, pasos };
  }

  /** Exp 3 — Idempotencia: reenvía el mismo eventId con simular-evento. */
  private async expIdempotencia(): Promise<ResultadoExperimento> {
    const pasos: string[] = [];
    const prosumidor = `PRO-EXP3-${Date.now()}`;
    const ahora = new Date().toISOString();
    const evento: SimularConsumoEvento = {
      eventId: `EVT-IDEMP-${Date.now()}`,
      medidorId: `MED-EXP3-${Date.now()}`,
      prosumidorId: prosumidor,
      consumoKwh: 150,
      umbralKwh: 100,
      consumoExtra: true,
      ocurridoEn: ahora,
      emitidoEn: ahora,
    };

    await firstValueFrom(this.tarifaSvc.simularEvento(evento));
    await this.demora(300);
    const antes = (await firstValueFrom(this.tarifaSvc.porProsumidor(prosumidor))).length;
    pasos.push(`1er envío del evento ${evento.eventId} → cargos: ${antes}`);
    this.evento(
      'ConsumoRegistrado (simulado)',
      'tarifa-eventos-service',
      `${prosumidor} · ${evento.consumoKwh} kWh · eventId ${evento.eventId}`,
      'emitido',
      `sim:${evento.eventId}`,
    );

    // Reenvío con el MISMO eventId.
    await firstValueFrom(this.tarifaSvc.simularEvento(evento));
    await this.demora(300);
    const despues = (await firstValueFrom(this.tarifaSvc.porProsumidor(prosumidor))).length;
    pasos.push(`Reenvío con el MISMO eventId → cargos: ${despues}`);
    this.evento(
      'Reenvío idempotente',
      'tarifa-eventos-service',
      `${prosumidor} · mismo eventId ${evento.eventId} · cargos ${antes}→${despues} (sin duplicar)`,
      'consumido',
      `sim-dup:${evento.eventId}`,
    );

    const ok = despues === antes && antes >= 1;
    return {
      ok,
      detalle: ok
        ? `0 duplicados: ${antes} cargo(s) antes y después del reenvío. Idempotencia correcta.`
        : `Cambió el conteo (${antes} → ${despues}); posible duplicado.`,
      pasos,
    };
  }

  /** Exp 4 — Notificación liquidada al activar medidor. */
  private async expNotificacion(): Promise<ResultadoExperimento> {
    const pasos: string[] = [];
    const notif = await firstValueFrom(
      this.tarifaSvc.activarMedidor({
        medidorId: `MED-EXP4-${Date.now()}`,
        prosumidorId: `PRO-EXP4-${Date.now()}`,
        umbralKwh: 120,
      }),
    );
    pasos.push(`Notificación ${notif.notificacionId} · tipo ${notif.tipo}`);
    pasos.push(`Cargo fijo: ${notif.cargoFijoMensual} COP/mes · Precio base: ${notif.precioBaseKwh} COP/kWh`);
    pasos.push(`Precio extra: ${notif.precioExtraKwh} COP/kWh · Umbral: ${notif.umbralKwh} kWh`);
    this.evento(
      'NotificacionLiquidada',
      'tarifa-eventos-service',
      `${notif.prosumidorId} · ${notif.tipo} · fijo ${notif.cargoFijoMensual} · base ${notif.precioBaseKwh} · extra ${notif.precioExtraKwh}`,
      'emitido',
      `notif:${notif.notificacionId}`,
    );
    const ok =
      notif.tipo === 'MEDIDOR_ACTIVADO' &&
      notif.cargoFijoMensual > 0 &&
      notif.precioBaseKwh > 0 &&
      notif.precioExtraKwh > 0;
    return {
      ok,
      detalle: ok
        ? 'Notificación liquidada generada con factura y tarifas y publicada al tópico de notificación.'
        : 'La notificación no contiene todas las tarifas esperadas.',
      pasos,
    };
  }

  private async buscarCargos(medidorId: string) {
    const todos = await firstValueFrom(this.tarifaSvc.listar());
    return todos.filter((c) => c.medidorId === medidorId);
  }
}
