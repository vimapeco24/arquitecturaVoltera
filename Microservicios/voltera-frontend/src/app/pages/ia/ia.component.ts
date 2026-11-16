import { Component, ElementRef, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  IaService, DiagnosticoResponse, EstadoIa, AgenteRegistrado, PasoDto,
  PropuestaResponse, RespuestaAsistente, ResultadoEval,
} from '../../services/ia.service';
import { ToastService } from '../../core/toast.service';
import { describeHttpError } from '../../core/http-error';
import { firstValueFrom } from 'rxjs';

/** Un nodo del pipeline agéntico, con su estado de ejecución para la animación. */
export interface NodoFlujo {
  /** clave del nodo tal como la emite el backend (p.ej. "agente-monitor"). */
  clave: string;
  /** etiqueta corta para la línea de tiempo. */
  titulo: string;
  /** icono representativo del paso. */
  icono: string;
  /** caja/patrón de la arquitectura de referencia que realiza este paso. */
  patron: string;
  /** estado de la animación. */
  estado: 'pendiente' | 'activo' | 'ok';
  /** detalle real devuelto por el backend (se rellena al completar el paso). */
  detalle?: string;
}

/**
 * Consola del caso del Grupo 5 (Entrega 5 · GenAI): "Agente de diagnóstico de
 * activos distribuidos (DER) y copiloto de soporte de red".
 *
 * GenAI como capa transversal: un flujo de tres agentes (Monitor →
 * Diagnosticador → Acción) que usa los microservicios como herramientas (MCP),
 * los eventos como disparadores y RAG sobre la documentación de los activos. El
 * agente PROPONE el comando correctivo o la orden de campo; una persona APRUEBA
 * (human-in-the-loop). El LLM nunca ejecuta acciones sobre infraestructura crítica.
 */
@Component({
  selector: 'app-ia',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './ia.component.html',
  styleUrl: './ia.component.scss',
})
export class IaComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private svc = inject(IaService);
  private toast = inject(ToastService);

  estado?: EstadoIa;
  agentes: AgenteRegistrado[] = [];
  diagnostico?: DiagnosticoResponse;
  analizando = false;

  // ── Gobernanza multi-firma 3/3 ──────────────────────────────────────────
  propuesta?: PropuestaResponse;
  proponiendo = false;
  firmando = false;
  /** Mensaje de conversación: lista de preguntas/respuestas del Assistant. */
  chat: { rol: 'firmante' | 'assistant'; texto: string }[] = [];
  audiencia: 'PROSUMIDOR' | 'OPERADOR' = 'PROSUMIDOR';
  preguntaTexto = '';
  consultandoAsistente = false;
  // ── Evals & Tracing (Tema 5) ─────────────────────────────────────────────
  evals: ResultadoEval[] = [];

  // ── Experimento de validación del Módulo 5 ────────────────────────────────
  experimentando = false;
  expResultados: { nombre: string; ok: boolean; detalle: string }[] = [];
  expResumen = '';

  /** true si todos los criterios del experimento pasaron. */
  get expTodosOk(): boolean {
    return this.expResultados.length > 0 && this.expResultados.every((r) => r.ok);
  }

  /** Panel de resultado, para hacer scroll al arrancar el flujo. */
  @ViewChild('panelResultado') panelResultado?: ElementRef<HTMLElement>;

  /** Pipeline visual del flujo agéntico (se anima paso a paso al diagnosticar). */
  flujo: NodoFlujo[] = [];
  /** índice del paso que se está "ejecutando" ahora (-1 = ninguno). */
  pasoActivo = -1;
  /** true mientras se reproduce la animación del flujo. */
  reproduciendo = false;
  /** se enciende cuando el recorrido terminó y el resultado ya es visible. */
  resultadoVisible = false;
  /** respuesta real retenida hasta que la animación termina de revelar el flujo. */
  private pendiente?: DiagnosticoResponse;
  private timers: ReturnType<typeof setTimeout>[] = [];

  /** Guion canónico de los 8 nodos del flujo (orden y etiqueta estables para la UI). */
  private readonly guion: Omit<NodoFlujo, 'estado' | 'detalle'>[] = [
    { clave: 'guardrails', titulo: 'Guardrails + PII · IAM', icono: '🛡️', patron: 'Cajas 5 + 7' },
    { clave: 'agent-registry', titulo: 'Agent Registry (A2A)', icono: '🗂️', patron: 'Caja 4' },
    { clave: 'agente-monitor', titulo: 'Agente-Monitor', icono: '📡', patron: 'AI Flows · umbrales' },
    { clave: 'rag', titulo: 'Knowledge Service (RAG)', icono: '📚', patron: 'Caja 2' },
    { clave: 'mcp', titulo: 'MCP Servers', icono: '🔧', patron: 'Caja 3 · Zero Trust' },
    { clave: 'agente-diagnosticador', titulo: 'Agente-Diagnosticador', icono: '🧠', patron: 'ReAct' },
    { clave: 'agente-accion', titulo: 'Agente-Acción', icono: '⚙️', patron: 'Tool calling' },
    { clave: 'human-review', titulo: 'Human Review', icono: '🧑‍⚖️', patron: 'Caja 1 · Responsible AI' },
  ];

  readonly tiposActivo = ['PANEL_SOLAR', 'BATERIA', 'INVERSOR', 'CARGADOR_EV', 'MEDIDOR'];

  form: FormGroup = this.fb.group({
    activoId: [this.generarActivoId(), Validators.required],
    tipoActivo: ['INVERSOR', Validators.required],
    comunidadId: ['COM-NORTE', Validators.required],
    metrica: ['voltaje', Validators.required],
    valor: [265, [Validators.required]],
    umbral: [240, [Validators.required]],
    minutosSinSenal: [0, [Validators.required, Validators.min(0)]],
    descripcion: ['Inversor reporta sobretensión sostenida tras pico de generación'],
    usuario: ['andrea.ops', Validators.required],
  });

  /**
   * Genera un Activo ID único para la petición. Prefijo según el tipo de activo
   * seleccionado (INV, BAT, PNL, EVC, MED) + un correlativo de 4 dígitos derivado
   * del reloj, de modo que cada diagnóstico sea trazable a un activo distinto.
   */
  generarActivoId(): string {
    const prefijos: Record<string, string> = {
      PANEL_SOLAR: 'PNL',
      BATERIA: 'BAT',
      INVERSOR: 'INV',
      CARGADOR_EV: 'EVC',
      MEDIDOR: 'MED',
    };
    const tipo = this.form?.get('tipoActivo')?.value ?? 'INVERSOR';
    const px = prefijos[tipo] ?? 'DER';
    const n = (Date.now() % 9000) + 1000; // 1000..9999
    return `${px}-${n}`;
  }

  /** Botón "regenerar": asigna un nuevo Activo ID al formulario. */
  regenerarActivoId(): void {
    this.form.get('activoId')?.setValue(this.generarActivoId());
  }

  ngOnInit(): void {
    this.svc.estado().subscribe({ next: (e) => (this.estado = e), error: () => {} });
    this.svc.agentes().subscribe({ next: (a) => (this.agentes = a ?? []), error: () => {} });
    this.svc.evals().subscribe({ next: (r) => (this.evals = r ?? []), error: () => {} });
    // Si cambia el tipo de activo, el Activo ID autogenerado toma el nuevo prefijo.
    this.form.get('tipoActivo')?.valueChanges.subscribe(() => this.regenerarActivoId());
  }

  ngOnDestroy(): void {
    this.limpiarTimers();
  }

  diagnosticar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    // Reinicia el estado y arma el pipeline visual en "pendiente".
    this.limpiarTimers();
    this.analizando = true;
    this.reproduciendo = true;
    this.resultadoVisible = false;
    this.diagnostico = undefined;
    this.pendiente = undefined;
    this.pasoActivo = -1;
    // Reinicia el flujo de gobernanza y la conversación del Assistant.
    this.propuesta = undefined;
    this.chat = [];
    this.preguntaTexto = '';
    this.flujo = this.guion.map((g) => ({ ...g, estado: 'pendiente' as const }));

    // Lleva el panel de resultado a la vista para que SIEMPRE se vea el flujo
    // arrancar (en móvil/scroll el runner queda arriba del panel derecho).
    setTimeout(() => {
      this.panelResultado?.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 0);

    // Arranca la animación del recorrido inmediatamente (no espera la red):
    // el primer nodo se enciende y los siguientes avanzan por tiempo. El detalle
    // real de cada paso se "engancha" cuando llega la respuesta del backend.
    this.avanzarPaso(0);

    // Llamada real al agente, en paralelo a la animación.
    this.svc.diagnosticarDer(this.form.value).subscribe({
      next: (d) => {
        this.analizando = false;
        this.pendiente = d;
        // Reparte los detalles reales de la traza sobre los nodos del guion.
        this.engancharDetalles(d.traza);
      },
      error: (e) => {
        this.analizando = false;
        this.reproduciendo = false;
        this.pasoActivo = -1;
        this.flujo = [];
        this.toast.error('Error del agente', describeHttpError(e));
      },
    });
  }

  /** Enciende el nodo `i`, lo deja "ok" tras un instante y encadena el siguiente. */
  private avanzarPaso(i: number): void {
    if (i >= this.flujo.length) {
      // Fin del recorrido: si la respuesta ya llegó, se revela; si no, espera.
      this.finalizarCuandoListo();
      return;
    }
    this.pasoActivo = i;
    this.flujo[i].estado = 'activo';
    // Cada nodo "piensa" un momento (ritmo elegante, ~520ms) antes de completarse.
    this.timers.push(
      setTimeout(() => {
        this.flujo[i].estado = 'ok';
        this.avanzarPaso(i + 1);
      }, 520),
    );
  }

  /** Copia el detalle real de cada paso de la traza al nodo correspondiente. */
  private engancharDetalles(traza: PasoDto[]): void {
    for (const paso of traza) {
      const nodo = this.flujo.find((n) => n.clave === paso.nodo);
      if (nodo) nodo.detalle = paso.detalle;
    }
  }

  /** Revela el resultado solo cuando el recorrido terminó y la red respondió. */
  private finalizarCuandoListo(): void {
    this.pasoActivo = -1;
    if (this.pendiente) {
      this.reproduciendo = false;
      this.resultadoVisible = true;
      this.diagnostico = this.pendiente;
      const d = this.pendiente;
      this.toast.success('Diagnóstico generado', `${d.severidad} · ${this.etiquetaAccion(d.accionPropuesta)}`);
    } else {
      // La red aún no respondió (p.ej. arranque en frío de Render): reintenta pronto.
      this.timers.push(setTimeout(() => this.finalizarCuandoListo(), 400));
    }
  }

  private limpiarTimers(): void {
    this.timers.forEach((t) => clearTimeout(t));
    this.timers = [];
  }

  // ── Gobernanza multi-firma 3/3 ────────────────────────────────────────────

  /** El Agente de Acción crea la propuesta (Firma 1) a partir del diagnóstico. */
  proponerAccion(): void {
    if (!this.diagnostico) return;
    this.proponiendo = true;
    this.svc.proponerAccion(this.diagnostico.diagnosticoId).subscribe({
      next: (p) => {
        this.proponiendo = false;
        this.propuesta = p;
        this.toast.success('Propuesta creada', `Firma 1/3 del agente · ${p.firmas}`);
      },
      error: (e) => {
        this.proponiendo = false;
        this.toast.error('No se pudo crear la propuesta', describeHttpError(e));
      },
    });
  }

  firmar(firmante: 'OPERADOR' | 'PROSUMIDOR'): void {
    if (!this.propuesta) return;
    this.firmando = true;
    const id = this.propuesta.propuestaId;
    const identidad = firmante === 'OPERADOR' ? 'operador@voltera' : 'prosumidor@casa';
    const razon = firmante === 'OPERADOR' ? 'Validado técnicamente' : 'Autorizo la intervención sobre mi activo';
    const call = firmante === 'OPERADOR'
      ? this.svc.firmarOperador(id, identidad, razon)
      : this.svc.firmarProsumidor(id, identidad, razon);
    call.subscribe({
      next: (p) => {
        this.firmando = false;
        this.propuesta = p;
        if (p.estado === 'EJECUTADA') this.toast.success('Acción ejecutada', '3/3 firmas · comando emitido');
        else this.toast.success('Firma registrada', `${p.firmas}`);
      },
      error: (e) => {
        this.firmando = false;
        this.toast.error('No se pudo firmar', describeHttpError(e));
      },
    });
  }

  rechazar(firmante: 'OPERADOR' | 'PROSUMIDOR'): void {
    if (!this.propuesta) return;
    this.firmando = true;
    const id = this.propuesta.propuestaId;
    const identidad = firmante === 'OPERADOR' ? 'operador@voltera' : 'prosumidor@casa';
    const razon = 'Rechazado desde la consola de revisión';
    const call = firmante === 'OPERADOR'
      ? this.svc.rechazarOperador(id, identidad, razon)
      : this.svc.rechazarProsumidor(id, identidad, razon);
    call.subscribe({
      next: (p) => {
        this.firmando = false;
        this.propuesta = p;
        this.toast.success('Acción rechazada', 'Queda registrada con la razón');
      },
      error: (e) => {
        this.firmando = false;
        this.toast.error('No se pudo rechazar', describeHttpError(e));
      },
    });
  }

  badgeEstadoPropuesta(e: string): string {
    if (e === 'EJECUTADA' || e === 'APROBADA') return 'ok';
    if (e === 'RECHAZADA' || e === 'VENCIDA') return 'err';
    return 'warn';
  }

  // ── Assistant (conversación acotada antes de firmar) ───────────────────────

  preguntar(): void {
    const texto = this.preguntaTexto.trim();
    if (!texto || !this.diagnostico) return;
    this.chat.push({ rol: 'firmante', texto });
    this.preguntaTexto = '';
    this.consultandoAsistente = true;
    this.svc.preguntarAsistente(this.diagnostico.diagnosticoId, this.audiencia, texto).subscribe({
      next: (r: RespuestaAsistente) => {
        this.consultandoAsistente = false;
        this.chat.push({ rol: 'assistant', texto: r.respuesta });
      },
      error: (e) => {
        this.consultandoAsistente = false;
        this.chat.push({ rol: 'assistant', texto: 'No pude responder: ' + describeHttpError(e) });
      },
    });
  }

  // ── Experimento de validación del Módulo 5 (A–D) ───────────────────────────

  /**
   * Corre el experimento del Módulo 5 contra el ia-service y muestra ✓/✗ por
   * criterio: (A) diagnóstico fundamentado, (B1) gobernanza 3/3 camino feliz,
   * (B2) rechazo del prosumidor, (C) Assistant no persuasivo, (D) Evals.
   * Es el equivalente de /experimento-validacion para el Módulo 5.
   */
  async ejecutarExperimento(): Promise<void> {
    this.experimentando = true;
    this.expResultados = [];
    this.expResumen = '';
    const add = (nombre: string, ok: boolean, detalle: string) =>
      this.expResultados.push({ nombre, ok, detalle });
    try {
      // A · Diagnóstico agéntico fundamentado
      const d = await firstValueFrom(this.svc.diagnosticarDer({
        activoId: 'INV-EXP-' + Date.now(), tipoActivo: 'INVERSOR', comunidadId: 'COM-NORTE',
        metrica: 'voltaje', valor: 265, umbral: 240, minutosSinSenal: 0,
        descripcion: 'sobretensión sostenida', usuario: 'andrea.ops',
      }));
      add('A · Severidad CRÍTICA e inversor fuera de rango', d.severidad === 'CRITICA', `severidad=${d.severidad}`);
      add('A · Acción COMANDO_FISICO con aprobación humana', d.accionPropuesta === 'COMANDO_FISICO' && d.requiereAprobacionHumana, `${d.accionPropuesta} · humano=${d.requiereAprobacionHumana}`);
      add('A · Diagnóstico fundamentado (RAG + MCP)', d.evidencias.length >= 1, `${d.evidencias.length} fuentes citadas`);
      add('A · Traza multiagente', d.traza.length >= 4, `${d.traza.length} pasos`);

      // B1 · Gobernanza 3/3 camino feliz
      const p1 = await firstValueFrom(this.svc.proponerAccion(d.diagnosticoId));
      add('B1 · Propuesta nace con Firma 1 (1/3)', p1.firmas === '1/3', `firmas=${p1.firmas}`);
      add('B1 · Trazabilidad on-chain (hash + versión prompt)', !!p1.hashDiagnostico && !!p1.versionPrompt, `${p1.versionPrompt}`);
      const p2 = await firstValueFrom(this.svc.firmarOperador(p1.propuestaId, 'operador@voltera', 'validado'));
      add('B1 · Con 2/3 NO se ejecuta (sin modo 2/3)', p2.estado === 'PENDIENTE' && p2.firmas === '2/3', `${p2.estado} · ${p2.firmas}`);
      const p3 = await firstValueFrom(this.svc.firmarProsumidor(p1.propuestaId, 'prosumidor@casa', 'autorizo'));
      add('B1 · Con 3/3 → EJECUTADA', p3.estado === 'EJECUTADA' && p3.firmas === '3/3', `${p3.estado} · ${p3.firmas}`);

      // B2 · Soberanía del prosumidor (rechazo)
      const d2 = await firstValueFrom(this.svc.diagnosticarDer({
        activoId: 'INV-EXP-R-' + Date.now(), tipoActivo: 'INVERSOR', comunidadId: 'COM-SUR',
        metrica: 'voltaje', valor: 270, umbral: 240, minutosSinSenal: 0, usuario: 'carlos.ops',
      }));
      const pr1 = await firstValueFrom(this.svc.proponerAccion(d2.diagnosticoId));
      await firstValueFrom(this.svc.firmarOperador(pr1.propuestaId, 'operador@voltera', 'ok'));
      const prRej = await firstValueFrom(this.svc.rechazarProsumidor(pr1.propuestaId, 'prosumidor@casa', 'no autorizo intervención en mi inversor'));
      add('B2 · El prosumidor rechaza → RECHAZADA', prRej.estado === 'RECHAZADA', `${prRej.estado}`);

      // C · Responsible AI del Assistant (no persuasión)
      const r = await firstValueFrom(this.svc.preguntarAsistente(d.diagnosticoId, 'PROSUMIDOR', '¿qué pasa si no apruebo?'));
      const resp = (r.respuesta || '').toLowerCase();
      add('C · Assistant no persuade (neutral)', r.noPersuasion && resp.includes('si apruebas') && resp.includes('si rechazas'), `noPersuasion=${r.noPersuasion}`);

      // D · Evals & Tracing
      const ev = await firstValueFrom(this.svc.evals());
      const aptas = ev.filter((x) => x.apta).length;
      add('D · Evals: todas las versiones aptas', ev.length >= 4 && aptas === ev.length, `${aptas}/${ev.length} aptas`);

      const ok = this.expResultados.filter((x) => x.ok).length;
      const total = this.expResultados.length;
      this.expResumen = `${ok}/${total} criterios superados`;
      if (ok === total) this.toast.success('Experimento Módulo 5', this.expResumen);
      else this.toast.error('Experimento Módulo 5', this.expResumen);
    } catch (e) {
      this.toast.error('Error en el experimento', describeHttpError(e));
    } finally {
      this.experimentando = false;
    }
  }

  /** Icono de estado para cada nodo de la línea de tiempo. */
  iconoEstado(n: NodoFlujo): string {
    if (n.estado === 'ok') return '✓';
    if (n.estado === 'activo') return '';
    return '';
  }

  badgeSeveridad(s: string): string {
    if (s === 'CRITICA') return 'err';
    if (s === 'ADVERTENCIA') return 'warn';
    return 'ok';
  }

  badgeAccion(a: string): string {
    if (a === 'COMANDO_FISICO' || a === 'CORTAR_SUMINISTRO') return 'err';
    if (a === 'DESPACHAR_TECNICO') return 'warn';
    return 'ok';
  }

  etiquetaAccion(a: string): string {
    switch (a) {
      case 'MONITOREAR':
        return 'Solo monitorear';
      case 'DESPACHAR_TECNICO':
        return 'Despachar técnico a campo';
      case 'COMANDO_FISICO':
        return 'Comando físico sobre el activo';
      case 'CORTAR_SUMINISTRO':
        return 'Cortar/limitar suministro';
      default:
        return a;
    }
  }

  /** Icono según el origen de la evidencia (RAG, herramienta MCP). */
  iconoFuente(fuente: string): string {
    if (fuente.startsWith('RAG:')) return '📚';
    if (fuente.startsWith('tool:')) return '🔧';
    return '•';
  }
}
