import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReaseguroService } from '../../services/reaseguro.service';
import { ToastService } from '../../core/toast.service';
import { describeHttpError } from '../../core/http-error';
import { CesionVista, ReaseguroStats, SiniestroAprobadoEvento } from '../../core/models';

@Component({
  selector: 'app-reaseguro',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reaseguro.component.html',
  styleUrl: './domain-page.scss',
})
export class ReaseguroComponent {
  private svc = inject(ReaseguroService);
  private toast = inject(ToastService);

  cesiones: CesionVista[] = [];
  stats?: ReaseguroStats;
  cargando = false;
  simulando = false;

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.svc.listar().subscribe({
      next: (r) => { this.cesiones = r ?? []; this.cargando = false; },
      error: (e) => { this.cargando = false; this.toast.error('Error al consultar cesiones', this.msg(e)); },
    });
    this.svc.stats().subscribe({
      next: (s) => (this.stats = s),
      error: () => (this.stats = undefined),
    });
  }

  /** Simula el reenvío de un evento SiniestroAprobado (prueba de idempotencia). */
  simular(): void {
    this.simulando = true;
    const ahora = new Date().toISOString().slice(0, 19);
    const evento: SiniestroAprobadoEvento = {
      eventId: `EVT-SIM-${Date.now()}`,
      siniestroId: `SIN-SIM-${Date.now()}`,
      polizaId: 'POL-EDA-SIM',
      prosumidorId: 'PRO-EDA-SIM',
      montoAprobado: 1000000,
      descripcion: 'Evento simulado desde el frontend',
      aprobadoEn: ahora,
      emitidoEn: ahora,
    };
    this.svc.simularEvento(evento).subscribe({
      next: (c) => {
        this.simulando = false;
        this.toast.success('Evento simulado', `Cesión ${c.id} · ${c.estado}`);
        this.cargar();
      },
      error: (e) => { this.simulando = false; this.toast.error('Error al simular evento', this.msg(e)); },
    });
  }

  badge(estado: string): string {
    if (estado === 'CEDIDA') return 'ok';
    if (estado === 'RECHAZADA_REASEGURO') return 'err';
    return 'warn';
  }

  /**
   * % promedio cedido derivado de los montos que envía el backend
   * (montoTotalCedido / montoTotalAprobado). El backend no expone este dato
   * directamente en /stats, por lo que se calcula en el cliente.
   */
  get porcentajePromedioCedido(): number {
    const aprobado = this.stats?.montoTotalAprobado ?? 0;
    const cedido = this.stats?.montoTotalCedido ?? 0;
    return aprobado > 0 ? (cedido / aprobado) * 100 : 0;
  }

  private msg(e: any): string {
    return describeHttpError(e);
  }
}
