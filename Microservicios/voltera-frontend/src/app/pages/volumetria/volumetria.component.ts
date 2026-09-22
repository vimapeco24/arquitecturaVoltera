import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { VolumetriaService } from '../../services/volumetria.service';
import { ToastService } from '../../core/toast.service';
import { describeHttpError } from '../../core/http-error';
import { toNum, toLocalInput } from '../../core/num';
import { Lectura } from '../../core/models';

@Component({
  selector: 'app-volumetria',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './volumetria.component.html',
  styleUrl: './domain-page.scss',
})
export class VolumetriaComponent {
  private fb = inject(FormBuilder);
  private svc = inject(VolumetriaService);
  private toast = inject(ToastService);

  lecturas: Lectura[] = [];
  enviando = false;
  cargando = false;
  filtroMedidor = 'MED-001';
  editandoId?: string;

  form: FormGroup = this.fb.group({
    medidorId: ['MED-001', Validators.required],
    kwh: [2.5, [Validators.required, Validators.min(0)]],
    direccion: ['CONSUMO', Validators.required],
    capturadaEn: [toLocalInput(), Validators.required],
  });

  ingestar(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.enviando = true;
    const body = {
      ...this.form.value,
      kwh: toNum(this.form.value.kwh),
      capturadaEn: new Date(this.form.value.capturadaEn).toISOString(),
    };
    if (this.editandoId) {
      const id = this.editandoId;
      this.svc.actualizar(id, body).subscribe({
        next: (l) => {
          this.enviando = false;
          this.toast.success('Actualizado', `${l.medidorId} · ${l.estado}`);
          this.filtroMedidor = l.medidorId;
          this.cancelarEdicion();
          this.buscar();
        },
        error: (e) => { this.enviando = false; this.toast.error('Error al actualizar', this.msg(e)); },
      });
      return;
    }
    this.svc.ingestar(body).subscribe({
      next: (l) => {
        this.enviando = false;
        this.toast.success('Lectura ingestada', `${l.medidorId} · ${l.estado}`);
        this.filtroMedidor = l.medidorId;
        this.buscar();
      },
      error: (e) => { this.enviando = false; this.toast.error('Error al ingestar', this.msg(e)); },
    });
  }

  editar(l: Lectura): void {
    this.editandoId = l.id;
    this.form.patchValue({
      medidorId: l.medidorId,
      kwh: l.kwh,
      direccion: l.direccion,
      capturadaEn: l.capturadaEn ? toLocalInput(new Date(l.capturadaEn)) : toLocalInput(),
    });
    setTimeout(() => document.querySelector('form.panel')?.scrollIntoView({ behavior: 'smooth', block: 'start' }));
  }

  cancelarEdicion(): void {
    this.editandoId = undefined;
    this.form.reset({
      medidorId: 'MED-001',
      kwh: 2.5,
      direccion: 'CONSUMO',
      capturadaEn: toLocalInput(),
    });
  }

  eliminar(l: Lectura): void {
    if (!window.confirm('¿Eliminar este registro? Esta acción no se puede deshacer.')) return;
    this.svc.eliminar(l.id).subscribe({
      next: () => { this.toast.success('Eliminado'); this.buscar(); },
      error: (e) => this.toast.error('Error al eliminar', this.msg(e)),
    });
  }

  buscar(): void {
    if (!this.filtroMedidor) return;
    this.cargando = true;
    this.svc.porMedidor(this.filtroMedidor).subscribe({
      next: (r) => { this.lecturas = r ?? []; this.cargando = false; },
      error: (e) => { this.cargando = false; this.toast.error('Error al consultar', this.msg(e)); },
    });
  }

  badge(estado: string): string {
    if (estado === 'VALIDA') return 'ok';
    if (estado === 'SOSPECHOSA') return 'warn';
    return 'err';
  }

  private msg(e: any): string {
    return describeHttpError(e);
  }
}
