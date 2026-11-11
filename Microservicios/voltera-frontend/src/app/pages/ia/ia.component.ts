import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { IaService, DiagnosticoResponse, EstadoIa, AgenteRegistrado } from '../../services/ia.service';
import { ToastService } from '../../core/toast.service';
import { describeHttpError } from '../../core/http-error';

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
export class IaComponent implements OnInit {
  private fb = inject(FormBuilder);
  private svc = inject(IaService);
  private toast = inject(ToastService);

  estado?: EstadoIa;
  agentes: AgenteRegistrado[] = [];
  diagnostico?: DiagnosticoResponse;
  analizando = false;

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

  diagnosticar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.analizando = true;
    this.diagnostico = undefined;
    this.svc.diagnosticarDer(this.form.value).subscribe({
      next: (d) => {
        this.analizando = false;
        this.diagnostico = d;
        this.toast.success('Diagnóstico generado', `${d.severidad} · ${this.etiquetaAccion(d.accionPropuesta)}`);
      },
      error: (e) => {
        this.analizando = false;
        this.toast.error('Error del agente', describeHttpError(e));
      },
    });
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
