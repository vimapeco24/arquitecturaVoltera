import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { OrquestacionService } from '../../services/orquestacion.service';
import { TarifaEventosService } from '../../services/tarifa-eventos.service';
import { ToastService } from '../../core/toast.service';
import { describeHttpError } from '../../core/http-error';
import { InboxVista, OutboxMensaje, SagaAlta, SimularConsumoEvento } from '../../core/models';

/**
 * Página "Orquestación · Outbox · Inbox" — patrones de la lámina 10.
 *
 * Demuestra en vivo, contra el tarifa-eventos-service real:
 *  1. ORQUESTACIÓN (process manager / mediador): alta de medidor que publica
 *     MedidorHabilitado y espera CanalIngestaCreado + TarifaAsignada; si vencen
 *     15 min emite HabilitaciónFallida + MedidorSuspendido (botón "forzar timeout").
 *  2. OUTBOX transaccional: tabla de eventos salientes (pendiente/publicado).
 *  3. INBOX de idempotencia por consumidor: reenviar el MISMO eventId no duplica
 *     el cargo (se cuenta como duplicado).
 */
@Component({
  selector: 'app-orquestacion',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './orquestacion.component.html',
  styleUrl: './domain-page.scss',
})
export class OrquestacionComponent {
  private fb = inject(FormBuilder);
  private svc = inject(OrquestacionService);
  private tarifaSvc = inject(TarifaEventosService);
  private toast = inject(ToastService);

  sagas: SagaAlta[] = [];
  outboxFilas: OutboxMensaje[] = [];
  inboxVista?: InboxVista;
  cargando = false;

  /** eventId fijo para la demo de idempotencia (reenviar el mismo). */
  readonly eventIdDemo = `EVT-IDEMP-DEMO`;

  formAlta: FormGroup = this.fb.group({
    medidorId: ['MED-ALTA-001', Validators.required],
    prosumidorId: ['PRO-ALTA-001'],
  });

  ngOnInit(): void {
    this.refrescar();
  }

  refrescar(): void {
    this.cargando = true;
    this.svc.listarSagas().subscribe({
      next: (r) => { this.sagas = r ?? []; this.cargando = false; },
      error: (e) => { this.cargando = false; this.toast.error('Error al listar sagas', this.msg(e)); },
    });
    this.svc.outbox().subscribe({
      next: (r) => (this.outboxFilas = r ?? []),
      error: () => (this.outboxFilas = []),
    });
    this.svc.inbox().subscribe({
      next: (r) => (this.inboxVista = r),
      error: () => (this.inboxVista = undefined),
    });
  }

  iniciarAlta(): void {
    if (this.formAlta.invalid) { this.formAlta.markAllAsTouched(); return; }
    this.svc.iniciarAlta(this.formAlta.value).subscribe({
      next: (s) => {
        this.toast.success('Alta iniciada', `Saga ${s.sagaId} · MedidorHabilitado publicado (outbox)`);
        this.refrescar();
      },
      error: (e) => this.toast.error('Error al iniciar alta', this.msg(e)),
    });
  }

  canalIngesta(s: SagaAlta): void {
    this.svc.canalIngestaCreado(s.sagaId).subscribe({
      next: () => { this.toast.success('CanalIngestaCreado', `Saga ${s.sagaId}`); this.refrescar(); },
      error: (e) => this.toast.error('Error', this.msg(e)),
    });
  }

  tarifaAsignada(s: SagaAlta): void {
    this.svc.tarifaAsignada(s.sagaId).subscribe({
      next: () => { this.toast.success('TarifaAsignada', `Saga ${s.sagaId}`); this.refrescar(); },
      error: (e) => this.toast.error('Error', this.msg(e)),
    });
  }

  forzarTimeout(s: SagaAlta): void {
    this.svc.forzarTimeout(s.sagaId).subscribe({
      next: () => {
        this.toast.success('Timeout forzado', 'HabilitaciónFallida + MedidorSuspendido (compensación)');
        this.refrescar();
      },
      error: (e) => this.toast.error('Error', this.msg(e)),
    });
  }

  /** Idempotencia: envía DOS veces el MISMO eventId; el inbox descarta el segundo. */
  probarIdempotencia(): void {
    const ahora = new Date().toISOString();
    const evento: SimularConsumoEvento = {
      eventId: this.eventIdDemo,
      medidorId: 'MED-IDEMP',
      prosumidorId: 'PRO-IDEMP',
      consumoKwh: 150,
      umbralKwh: 100,
      consumoExtra: true,
      ocurridoEn: ahora,
      emitidoEn: ahora,
    };
    // Primer envío.
    this.tarifaSvc.simularEvento(evento).subscribe({
      next: () => {
        // Reenvío del mismo eventId (reentrega al-menos-una-vez).
        this.tarifaSvc.simularEvento(evento).subscribe({
          next: () => {
            this.toast.success('Idempotencia probada', 'Mismo eventId enviado 2 veces → 1 cargo, 1 duplicado');
            this.refrescar();
          },
          error: (e) => this.toast.error('Error en reenvío', this.msg(e)),
        });
      },
      error: (e) => this.toast.error('Error al simular', this.msg(e)),
    });
  }

  estadoBadge(estado: string): string {
    if (estado === 'COMPLETADA') return 'ok';
    if (estado === 'FALLIDA') return 'err';
    return 'warn';
  }

  private msg(e: any): string {
    return describeHttpError(e);
  }
}
