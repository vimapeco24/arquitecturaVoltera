import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { TelemetriaService } from '../../services/telemetria.service';
import { ToastService } from '../../core/toast.service';
import { describeHttpError } from '../../core/http-error';
import { withNums } from '../../core/num';
import { IngestaResponse, Medidor } from '../../core/models';

@Component({
  selector: 'app-telemetria',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './telemetria.component.html',
  styleUrl: './domain-page.scss',
})
export class TelemetriaComponent {
  private fb = inject(FormBuilder);
  private svc = inject(TelemetriaService);
  private toast = inject(ToastService);

  medidores: Medidor[] = [];
  seleccionado?: Medidor;
  ultimaIngesta?: IngestaResponse;
  bufferPendientes = 0;
  enviando = false;
  cargando = false;

  formMedidor: FormGroup = this.fb.group({
    prosumidorId: ['PRO-EDA-001', Validators.required],
    umbralKwh: [100, [Validators.required, Validators.min(0)]],
  });

  formLectura: FormGroup = this.fb.group({
    consumoKwh: [120, [Validators.required, Validators.min(0)]],
  });

  ngOnInit(): void {
    this.listar();
  }

  registrar(): void {
    if (this.formMedidor.invalid) { this.formMedidor.markAllAsTouched(); return; }
    this.enviando = true;
    const body = withNums(this.formMedidor.value, ['umbralKwh']);
    this.svc.registrar(body).subscribe({
      next: (m) => {
        this.enviando = false;
        this.seleccionado = m;
        this.toast.success('Medidor registrado', `${m.id} · ${m.estado}`);
        this.listar();
      },
      error: (e) => { this.enviando = false; this.toast.error('Error al registrar', this.msg(e)); },
    });
  }

  activar(m: Medidor): void {
    this.svc.activar(m.id).subscribe({
      next: (r) => {
        this.seleccionado = r;
        this.toast.success('Medidor activado', `${r.id} · ${r.estado}`);
        this.listar();
      },
      error: (e) => this.toast.error('Error al activar', this.msg(e)),
    });
  }

  suspender(m: Medidor): void {
    this.svc.suspender(m.id).subscribe({
      next: (r) => { this.seleccionado = r; this.toast.success('Medidor suspendido', r.id); this.listar(); },
      error: (e) => this.toast.error('Error al suspender', this.msg(e)),
    });
  }

  /** Envía una lectura de consumo del medidor seleccionado (emite ConsumoRegistrado). */
  ingestar(m: Medidor): void {
    if (this.formLectura.invalid) { this.formLectura.markAllAsTouched(); return; }
    const body = { ...withNums(this.formLectura.value, ['consumoKwh']), capturadaEn: new Date().toISOString() };
    this.svc.ingestar(m.id, body).subscribe({
      next: (r) => {
        this.ultimaIngesta = r;
        if (r.publicadoEnBroker) {
          this.toast.success('Lectura publicada al broker', `${r.consumoKwh} kWh · extra: ${r.consumoExtra}`);
        } else {
          this.toast.error('Sin conexión — lectura en buffer offline', 'Se reenviará al reconectar (store-and-forward)');
        }
      },
      error: (e) => this.toast.error('Error al ingestar lectura', this.msg(e)),
    });
  }

  /** Fuerza el desencolado del buffer offline (store-and-forward). */
  drenarBuffer(): void {
    this.svc.drenarBuffer().subscribe({
      next: (r) => {
        this.bufferPendientes = r.pendientes;
        this.toast.success('Buffer drenado', `Reenviados: ${r.reenviados} · Pendientes: ${r.pendientes}`);
      },
      error: (e) => this.toast.error('Error al drenar buffer', this.msg(e)),
    });
  }

  listar(): void {
    this.cargando = true;
    this.svc.listar().subscribe({
      next: (r) => { this.medidores = r ?? []; this.cargando = false; },
      error: (e) => { this.cargando = false; this.toast.error('Error al consultar', this.msg(e)); },
    });
  }

  seleccionar(m: Medidor): void {
    this.seleccionado = m;
  }

  puedeMedir(m: Medidor): boolean {
    return m.estado === 'ACTIVO';
  }

  badge(estado: string): string {
    if (estado === 'ACTIVO') return 'ok';
    if (estado === 'SUSPENDIDO') return 'err';
    return 'warn';
  }

  private msg(e: any): string {
    return describeHttpError(e);
  }
}
