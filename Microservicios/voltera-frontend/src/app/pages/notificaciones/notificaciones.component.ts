import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Notificacion, NotificacionesService } from '../../services/notificaciones.service';
import { ToastService } from '../../core/toast.service';
import { describeHttpError } from '../../core/http-error';

/**
 * Página "Notificaciones" (consumidor puro). Reacciona a MedidorHabilitado,
 * LecturaSospechosaDetectada, MedidorSinReporte y FacturaEmitida; emite
 * ClienteNotificado. Aquí se simulan esos eventos de entrada.
 */
@Component({
  selector: 'app-notificaciones',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './notificaciones.component.html',
  styleUrl: './domain-page.scss',
})
export class NotificacionesComponent {
  private fb = inject(FormBuilder);
  private svc = inject(NotificacionesService);
  private toast = inject(ToastService);

  historial: Notificacion[] = [];
  cargando = false;

  form: FormGroup = this.fb.group({
    medidorSerial: ['SER-001', Validators.required],
    consumoKwh: [999],
    motivo: ['Fuera de rango'],
  });

  ngOnInit(): void { this.cargar(); }

  habilitado(): void {
    this.svc.simularHabilitado(this.form.value.medidorSerial, this.form.value.medidorSerial).subscribe({
      next: () => { this.toast.success('MedidorHabilitado → notificación', ''); this.cargar(); },
      error: (e) => this.toast.error('Error', this.msg(e)),
    });
  }
  sospechosa(): void {
    const v = this.form.value;
    this.svc.simularSospechosa(v.medidorSerial, Number(v.consumoKwh), v.motivo).subscribe({
      next: () => { this.toast.success('LecturaSospechosa → notificación', ''); this.cargar(); },
      error: (e) => this.toast.error('Error', this.msg(e)),
    });
  }
  sinReporte(): void {
    this.svc.simularSinReporte(this.form.value.medidorSerial, new Date().toISOString()).subscribe({
      next: () => { this.toast.success('MedidorSinReporte → notificación', ''); this.cargar(); },
      error: (e) => this.toast.error('Error', this.msg(e)),
    });
  }
  factura(): void {
    this.svc.simularFactura(this.form.value.medidorSerial, 'FAC-' + Date.now(), '2026-10', 123.45).subscribe({
      next: () => { this.toast.success('FacturaEmitida → notificación', ''); this.cargar(); },
      error: (e) => this.toast.error('Error', this.msg(e)),
    });
  }
  cargar(): void {
    this.cargando = true;
    this.svc.historial().subscribe({
      next: (r) => { this.historial = r ?? []; this.cargando = false; },
      error: (e) => { this.cargando = false; this.toast.error('Error al consultar', this.msg(e)); },
    });
  }
  private msg(e: any): string { return describeHttpError(e); }
}
