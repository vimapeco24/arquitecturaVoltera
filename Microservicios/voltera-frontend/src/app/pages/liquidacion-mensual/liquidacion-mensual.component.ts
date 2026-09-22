import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { LiquidacionMensualService } from '../../services/liquidacion-mensual.service';
import { ToastService } from '../../core/toast.service';
import { describeHttpError } from '../../core/http-error';
import { toNum, withNums } from '../../core/num';
import { LiquidacionMensual } from '../../core/models';

@Component({
  selector: 'app-liquidacion-mensual',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './liquidacion-mensual.component.html',
  styleUrl: './domain-page.scss',
})
export class LiquidacionMensualComponent {
  private fb = inject(FormBuilder);
  private svc = inject(LiquidacionMensualService);
  private toast = inject(ToastService);

  actual?: LiquidacionMensual;
  historico: LiquidacionMensual[] = [];
  enviando = false;
  cargando = false;
  filtroProsumidor = 'PRO-001';
  editandoId?: string;

  // movimiento
  movTipo: 'CONSUMO' | 'EXCEDENTE' = 'CONSUMO';
  movKwh = 100;

  form: FormGroup = this.fb.group({
    prosumidorId: ['PRO-001', Validators.required],
    anio: [2026, [Validators.required, Validators.min(2000)]],
    mes: [1, [Validators.required, Validators.min(1), Validators.max(12)]],
    precioConsumoKwh: [680, [Validators.required, Validators.min(0)]],
    precioExcedenteKwh: [420, [Validators.required, Validators.min(0)]],
  });

  abrir(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.enviando = true;
    const body = withNums(this.form.value, ['anio', 'mes', 'precioConsumoKwh', 'precioExcedenteKwh']);
    if (this.editandoId) {
      const id = this.editandoId;
      this.svc.actualizar(id, body).subscribe({
        next: (l) => {
          this.enviando = false;
          this.actual = l;
          this.toast.success('Actualizado', `${l.prosumidorId} · ${l.periodo}`);
          this.filtroProsumidor = l.prosumidorId;
          this.cancelarEdicion();
          this.buscar();
        },
        error: (e) => { this.enviando = false; this.toast.error('Error al actualizar', this.msg(e)); },
      });
      return;
    }
    this.svc.abrir(body).subscribe({
      next: (l) => {
        this.enviando = false;
        this.actual = l;
        this.toast.success('Liquidación abierta', `${l.prosumidorId} · ${l.periodo}`);
      },
      error: (e) => { this.enviando = false; this.toast.error('Error al abrir', this.msg(e)); },
    });
  }

  editar(l: LiquidacionMensual): void {
    this.editandoId = l.id;
    const partes = (l.periodo ?? '').split('-');
    const anio = partes.length >= 1 && partes[0] ? Number(partes[0]) : 2026;
    const mes = partes.length >= 2 && partes[1] ? Number(partes[1]) : 1;
    this.form.patchValue({
      prosumidorId: l.prosumidorId,
      anio,
      mes,
      precioConsumoKwh: l.precioConsumoKwh,
      precioExcedenteKwh: l.precioExcedenteKwh,
    });
    setTimeout(() => document.querySelector('form.panel')?.scrollIntoView({ behavior: 'smooth', block: 'start' }));
  }

  cancelarEdicion(): void {
    this.editandoId = undefined;
    this.form.reset({
      prosumidorId: 'PRO-001',
      anio: 2026,
      mes: 1,
      precioConsumoKwh: 680,
      precioExcedenteKwh: 420,
    });
  }

  eliminar(l: LiquidacionMensual): void {
    if (!window.confirm('¿Eliminar este registro? Esta acción no se puede deshacer.')) return;
    this.svc.eliminar(l.id).subscribe({
      next: () => { this.toast.success('Eliminado'); this.buscar(); },
      error: (e) => this.toast.error('Error al eliminar', this.msg(e)),
    });
  }

  registrar(): void {
    if (!this.actual) return;
    this.svc.registrarMovimiento(this.actual.id, { tipo: this.movTipo, kwh: toNum(this.movKwh) }).subscribe({
      next: (l) => { this.actual = l; this.toast.success('Movimiento registrado', `${this.movTipo} ${this.movKwh} kWh`); },
      error: (e) => this.toast.error('Error al registrar', this.msg(e)),
    });
  }

  cerrar(): void {
    if (!this.actual) return;
    this.svc.cerrar(this.actual.id).subscribe({
      next: (l) => { this.actual = l; this.toast.success('Liquidación cerrada', `Neto ${l.netoKwh} kWh`); },
      error: (e) => this.toast.error('Error al cerrar', this.msg(e)),
    });
  }

  buscar(): void {
    if (!this.filtroProsumidor) return;
    this.cargando = true;
    this.svc.porProsumidor(this.filtroProsumidor).subscribe({
      next: (r) => { this.historico = r ?? []; this.cargando = false; },
      error: (e) => { this.cargando = false; this.toast.error('Error al consultar', this.msg(e)); },
    });
  }

  private msg(e: any): string {
    return describeHttpError(e);
  }
}
