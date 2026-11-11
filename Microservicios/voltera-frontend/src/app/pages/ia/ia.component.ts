import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { IaService, DiagnosticoResponse, EstadoIa, AgenteRegistrado, PasoDto } from '../../services/ia.service';
import { ToastService } from '../../core/toast.service';
import { describeHttpError } from '../../core/http-error';

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
    activoId: ['INV-042', Validators.required],
    tipoActivo: ['INVERSOR', Validators.required],
    comunidadId: ['COM-NORTE', Validators.required],
    metrica: ['voltaje', Validators.required],
    valor: [265, [Validators.required]],
    umbral: [240, [Validators.required]],
    minutosSinSenal: [0, [Validators.required, Validators.min(0)]],
    descripcion: ['Inversor reporta sobretensión sostenida tras pico de generación'],
    usuario: ['andrea.ops', Validators.required],
  });

  ngOnInit(): void {
    this.svc.estado().subscribe({ next: (e) => (this.estado = e), error: () => {} });
    this.svc.agentes().subscribe({ next: (a) => (this.agentes = a ?? []), error: () => {} });
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
    this.flujo = this.guion.map((g) => ({ ...g, estado: 'pendiente' as const }));

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
