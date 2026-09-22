import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { SiniestrosService } from '../../services/siniestros.service';
import { ToastService } from '../../core/toast.service';
import { describeHttpError } from '../../core/http-error';
import { withNums } from '../../core/num';
import { Siniestro } from '../../core/models';

@Component({
  selector: 'app-siniestros',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './siniestros.component.html',
  styleUrl: './domain-page.scss',
})
export class SiniestrosComponent {
  private fb = inject(FormBuilder);
  private svc = inject(SiniestrosService);
  private toast = inject(ToastService);

  siniestros: Siniestro[] = [];
  ultimo?: Siniestro;
  ultimoAprobadoId?: string;
  enviando = false;
  cargando = false;

  form: FormGroup = this.fb.group({
    polizaId: ['POL-EDA-001', Validators.required],
    prosumidorId: ['PRO-EDA-001', Validators.required],
    descripcion: ['Daño por sobretensión en panel solar', Validators.required],
    montoReclamacion: [2500000, [Validators.required, Validators.min(0)]],
    fechaOcurrencia: ['2026-09-14T10:00:00', Validators.required],
  });

  reportar(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.enviando = true;
    const body = withNums(this.form.value, ['montoReclamacion']);
    this.svc.reportar(body).subscribe({
      next: (s) => {
        this.enviando = false;
        this.ultimo = s;
        this.ultimoAprobadoId = undefined;
        this.toast.success('Siniestro reportado', `${s.id} · ${s.estado}`);
        this.listar();
      },
      error: (e) => { this.enviando = false; this.toast.error('Error al reportar', this.msg(e)); },
    });
  }

  aprobar(s: Siniestro): void {
    this.svc.aprobar(s.id).subscribe({
      next: (r) => {
        this.ultimo = r;
        this.ultimoAprobadoId = r.id;
        this.toast.success('Siniestro aprobado', `${r.id} · evento SiniestroAprobado emitido`);
        this.listar();
      },
      error: (e) => this.toast.error('Error al aprobar', this.msg(e)),
    });
  }

  listar(): void {
    this.cargando = true;
    this.svc.listar().subscribe({
      next: (r) => { this.siniestros = r ?? []; this.cargando = false; },
      error: (e) => { this.cargando = false; this.toast.error('Error al consultar', this.msg(e)); },
    });
  }

  puedeAprobar(s: Siniestro): boolean {
    return s.estado === 'REPORTADO' || s.estado === 'EN_PERITAJE';
  }

  badge(estado: string): string {
    if (estado === 'APROBADO') return 'ok';
    if (estado === 'RECHAZADO') return 'err';
    return 'warn';
  }

  private msg(e: any): string {
    return describeHttpError(e);
  }
}
