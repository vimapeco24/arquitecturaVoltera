import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { HabilitacionService, MedidorHab } from '../../services/habilitacion.service';
import { ToastService } from '../../core/toast.service';
import { describeHttpError } from '../../core/http-error';

/**
 * Página "Habilitación de Medidores" (BC Habilitación · Transferencia de Estado).
 * Dispara el alta (OrdenInstalacionCerrada → MedidorHabilitado por OUTBOX con ECST),
 * confirma CanalIngestaCreado + TarifaAsignada (→ MedidorActivado) o fuerza el
 * timeout (→ HabilitaciónFallida + MedidorSuspendido).
 */
@Component({
  selector: 'app-habilitacion',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './habilitacion.component.html',
  styleUrl: './domain-page.scss',
})
export class HabilitacionComponent {
  private fb = inject(FormBuilder);
  private svc = inject(HabilitacionService);
  private toast = inject(ToastService);

  medidores: MedidorHab[] = [];
  cargando = false;

  form: FormGroup = this.fb.group({
    medidorId: ['MED-001', Validators.required],
    serial: ['SER-001', Validators.required],
    fabricante: ['Landis+Gyr'],
    codigoPunto: ['PM-001', Validators.required],
    direccion: ['Calle 1 # 2-3'],
  });

  ngOnInit(): void { this.listar(); }

  habilitar(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.svc.habilitar(this.form.value).subscribe({
      next: (m) => { this.toast.success('Alta disparada', `${m.medidorId} · ${m.estado} · MedidorHabilitado (ECST) en outbox`); this.listar(); },
      error: (e) => this.toast.error('Error al habilitar', this.msg(e)),
    });
  }
  canalIngesta(m: MedidorHab): void {
    this.svc.canalIngesta(m.medidorId).subscribe({
      next: (r) => { this.toast.success('CanalIngestaCreado', `${r.medidorId} · ${r.estado}`); this.listar(); },
      error: (e) => this.toast.error('Error', this.msg(e)),
    });
  }
  tarifaAsignada(m: MedidorHab): void {
    this.svc.tarifaAsignada(m.medidorId).subscribe({
      next: (r) => { this.toast.success('TarifaAsignada', `${r.medidorId} · ${r.estado}`); this.listar(); },
      error: (e) => this.toast.error('Error', this.msg(e)),
    });
  }
  forzarTimeout(m: MedidorHab): void {
    this.svc.forzarTimeout(m.medidorId).subscribe({
      next: (r) => { this.toast.success('Timeout forzado', `${r.medidorId} · ${r.estado}`); this.listar(); },
      error: (e) => this.toast.error('Error', this.msg(e)),
    });
  }
  listar(): void {
    this.cargando = true;
    this.svc.listar().subscribe({
      next: (r) => { this.medidores = r ?? []; this.cargando = false; },
      error: (e) => { this.cargando = false; this.toast.error('Error al consultar', this.msg(e)); },
    });
  }
  badge(estado: string): string {
    if (estado === 'ACTIVADO') return 'ok';
    if (estado === 'SUSPENDIDO') return 'err';
    return 'warn';
  }
  private msg(e: any): string { return describeHttpError(e); }
}
