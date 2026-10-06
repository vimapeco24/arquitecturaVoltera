import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { IngestaService, SesionIngesta } from '../../services/ingesta.service';
import { ToastService } from '../../core/toast.service';
import { describeHttpError } from '../../core/http-error';

/**
 * Página "Ingesta de Telemetría" (BC Telemetría · Ingesta). Valida la lectura contra
 * la regla y la ventana de duplicados; emite LecturaValidada o
 * LecturaSospechosaDetectada (esta última va además al topic telemetria-alertas).
 */
@Component({
  selector: 'app-ingesta',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './ingesta.component.html',
  styleUrl: './domain-page.scss',
})
export class IngestaComponent {
  private fb = inject(FormBuilder);
  private svc = inject(IngestaService);
  private toast = inject(ToastService);

  sesion?: SesionIngesta;
  ultimoResultado?: string;

  form: FormGroup = this.fb.group({
    medidorSerial: ['SER-001', Validators.required],
    consumoKwh: [45, [Validators.required, Validators.min(0)]],
  });

  ngOnInit(): void { this.cargarSesion(); }

  cargarSesion(): void {
    this.svc.sesion().subscribe({
      next: (s) => (this.sesion = s),
      error: (e) => this.toast.error('Error al consultar sesión', this.msg(e)),
    });
  }
  abrirCanal(): void {
    this.svc.abrirCanal(this.form.value.medidorSerial).subscribe({
      next: () => { this.toast.success('Canal abierto', 'CanalIngestaCreado emitido'); this.cargarSesion(); },
      error: (e) => this.toast.error('Error', this.msg(e)),
    });
  }
  ingestar(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.value;
    this.svc.ingestar(v.medidorSerial, Number(v.consumoKwh)).subscribe({
      next: (r) => {
        this.ultimoResultado = r.resultado;
        if (r.resultado === 'VALIDADA') this.toast.success('Lectura validada', 'LecturaValidada emitida');
        else if (r.resultado === 'SOSPECHOSA') this.toast.error('Lectura sospechosa', 'Fuera de rango → telemetria-alertas');
        else this.toast.success('Resultado', r.resultado);
      },
      error: (e) => this.toast.error('Error al ingestar', this.msg(e)),
    });
  }
  resultadoBadge(): string {
    if (this.ultimoResultado === 'VALIDADA') return 'ok';
    if (this.ultimoResultado === 'SOSPECHOSA') return 'err';
    return 'warn';
  }
  private msg(e: any): string { return describeHttpError(e); }
}
