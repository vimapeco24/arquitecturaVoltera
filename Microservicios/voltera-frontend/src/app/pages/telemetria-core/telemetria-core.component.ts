import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ConsumoAgregado, ConsumoVista, TelemetriaCoreService } from '../../services/telemetria-core.service';
import { ToastService } from '../../core/toast.service';
import { describeHttpError } from '../../core/http-error';

/**
 * Página "Telemetría Core · CQRS" (lámina 03). Lado COMMAND: inyecta LecturaValidada
 * (append-only). Lado QUERY: muestra la vista por intervalo y la vista AGREGADA
 * materializadas por el Proyector.
 */
@Component({
  selector: 'app-telemetria-core',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './telemetria-core.component.html',
  styleUrl: './domain-page.scss',
})
export class TelemetriaCoreComponent {
  private fb = inject(FormBuilder);
  private svc = inject(TelemetriaCoreService);
  private toast = inject(ToastService);

  vistas: ConsumoVista[] = [];
  agregados: ConsumoAgregado[] = [];
  cargando = false;

  form: FormGroup = this.fb.group({
    medidorSerial: ['SER-001', Validators.required],
    consumoKwh: [12, [Validators.required, Validators.min(0)]],
  });

  ngOnInit(): void { this.refrescar(); }

  simular(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.value;
    this.svc.simularLectura(v.medidorSerial, Number(v.consumoKwh)).subscribe({
      next: () => { this.toast.success('Lectura incorporada', 'Command → serie append-only'); this.refrescar(); },
      error: (e) => this.toast.error('Error al simular', this.msg(e)),
    });
  }
  refrescar(): void {
    this.cargando = true;
    this.svc.consumo().subscribe({
      next: (r) => { this.vistas = r ?? []; this.cargando = false; },
      error: (e) => { this.cargando = false; this.toast.error('Error (vista intervalo)', this.msg(e)); },
    });
    this.svc.consumoAgregado().subscribe({
      next: (r) => (this.agregados = Array.isArray(r) ? r : (r ? [r] : [])),
      error: () => (this.agregados = []),
    });
  }
  private msg(e: any): string { return describeHttpError(e); }
}
