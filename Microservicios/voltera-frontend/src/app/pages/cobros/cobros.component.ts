import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CobrosService } from '../../services/cobros.service';
import { ToastService } from '../../core/toast.service';
import { describeHttpError } from '../../core/http-error';
import { InboxVista, MovimientoVista, PagoConfirmadoEvento, RegistrarMovimientoRequest } from '../../core/models';

/**
 * Página "Cobros" (BC seguros · agregado Movimiento con Prima y Recibo).
 *
 * Alineada al cobros-service real: registra movimientos de cobro de prima,
 * los lista (lado lectura), simula el evento PagoConfirmado para conciliarlos
 * (emite recibo) y muestra la tabla INBOX de idempotencia.
 */
@Component({
  selector: 'app-cobros',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './cobros.component.html',
  styleUrl: '../reaseguro/domain-page.scss',
})
export class CobrosComponent {
  private svc = inject(CobrosService);
  private toast = inject(ToastService);

  movimientos: MovimientoVista[] = [];
  inboxVista?: InboxVista;
  cargando = false;
  simulando = false;
  registrando = false;

  /** Formulario de registro de un nuevo movimiento. */
  form: RegistrarMovimientoRequest = {
    clienteId: 'CLI-001',
    polizaId: 'POL-COBROS-001',
    montoPrima: 50000,
    periodicidad: 'MENSUAL',
  };

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.svc.listar().subscribe({
      next: (r) => { this.movimientos = r ?? []; this.cargando = false; },
      error: (e) => { this.cargando = false; this.toast.error('Error al consultar movimientos', this.msg(e)); },
    });
    this.svc.inbox().subscribe({
      next: (i) => (this.inboxVista = i),
      error: () => (this.inboxVista = undefined),
    });
  }

  registrar(): void {
    this.registrando = true;
    this.svc.registrar(this.form).subscribe({
      next: (m) => {
        this.registrando = false;
        this.toast.success('Movimiento registrado', `${m.id} · ${m.estado}`);
        this.cargar();
      },
      error: (e) => { this.registrando = false; this.toast.error('Error al registrar', this.msg(e)); },
    });
  }

  /** Simula un PagoConfirmado para la póliza del formulario (concilia y emite recibo). */
  simularPago(): void {
    this.simulando = true;
    // Instante ISO-8601 con zona ('Z'); el backend lo deserializa como Instant.
    // No recortar con slice(0,19): quitaría la 'Z' y provocaría 400 Bad Request.
    const ahora = new Date().toISOString();
    const evento: PagoConfirmadoEvento = {
      eventId: `EVT-PAGO-${Date.now()}`,
      referencia: `REC-${Date.now()}`,
      clienteId: this.form.clienteId,
      polizaId: this.form.polizaId,
      montoPagado: this.form.montoPrima,
      confirmadoEn: ahora,
      emitidoEn: ahora,
    };
    this.svc.simularPago(evento).subscribe({
      next: (m) => {
        this.simulando = false;
        this.toast.success('Pago confirmado procesado', `Movimiento ${m.id} · ${m.estado}`);
        this.cargar();
      },
      error: (e) => { this.simulando = false; this.toast.error('Error al simular pago', this.msg(e)); },
    });
  }

  badge(estado: string): string {
    return estado === 'CONCILIADO' ? 'ok' : 'warn';
  }

  private msg(e: any): string {
    return describeHttpError(e);
  }
}
