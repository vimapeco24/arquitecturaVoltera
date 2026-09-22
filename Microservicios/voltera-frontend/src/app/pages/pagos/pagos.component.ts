import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { PagosService } from '../../services/pagos.service';
import { ToastService } from '../../core/toast.service';
import { describeHttpError } from '../../core/http-error';
import { withNums } from '../../core/num';
import { Pago } from '../../core/models';

@Component({
  selector: 'app-pagos',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './pagos.component.html',
  styleUrl: './domain-page.scss',
})
export class PagosComponent {
  private fb = inject(FormBuilder);
  private svc = inject(PagosService);
  private toast = inject(ToastService);

  pagos: Pago[] = [];
  ultimo?: Pago;
  enviando = false;
  cargando = false;
  filtroProsumidor = 'PRO-001';
  editandoId?: string;

  form: FormGroup = this.fb.group({
    prosumidorId: ['PRO-001', Validators.required],
    referencia: ['FAC-100', Validators.required],
    tipo: ['COBRO_FACTURA', Validators.required],
    monto: [47600, [Validators.required, Validators.min(0)]],
  });

  procesar(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.enviando = true;
    const body = withNums(this.form.value, ['monto']);
    if (this.editandoId) {
      const id = this.editandoId;
      this.svc.actualizar(id, body).subscribe({
        next: (p) => {
          this.enviando = false;
          this.ultimo = p;
          this.toast.success('Actualizado', `${p.referencia} · ${p.estado}`);
          this.filtroProsumidor = p.prosumidorId;
          this.cancelarEdicion();
          this.buscar();
        },
        error: (e) => { this.enviando = false; this.toast.error('Error al actualizar', this.msg(e)); },
      });
      return;
    }
    this.svc.procesar(body).subscribe({
      next: (p) => {
        this.enviando = false;
        this.ultimo = p;
        this.toast.success('Pago procesado', `${p.referencia} · ${p.estado}`);
        this.filtroProsumidor = p.prosumidorId;
        this.buscar();
      },
      error: (e) => { this.enviando = false; this.toast.error('Error al procesar pago', this.msg(e)); },
    });
  }

  editar(p: Pago): void {
    this.editandoId = p.id;
    this.form.patchValue({
      prosumidorId: p.prosumidorId,
      referencia: p.referencia,
      tipo: p.tipo,
      monto: p.monto,
    });
    setTimeout(() => document.querySelector('form.panel')?.scrollIntoView({ behavior: 'smooth', block: 'start' }));
  }

  cancelarEdicion(): void {
    this.editandoId = undefined;
    this.form.reset({
      prosumidorId: 'PRO-001',
      referencia: 'FAC-100',
      tipo: 'COBRO_FACTURA',
      monto: 47600,
    });
  }

  eliminar(p: Pago): void {
    if (!window.confirm('¿Eliminar este registro? Esta acción no se puede deshacer.')) return;
    this.svc.eliminar(p.id).subscribe({
      next: () => { this.toast.success('Eliminado'); this.buscar(); },
      error: (e) => this.toast.error('Error al eliminar', this.msg(e)),
    });
  }

  buscar(): void {
    if (!this.filtroProsumidor) return;
    this.cargando = true;
    this.svc.porProsumidor(this.filtroProsumidor).subscribe({
      next: (r) => { this.pagos = r ?? []; this.cargando = false; },
      error: (e) => { this.cargando = false; this.toast.error('Error al consultar', this.msg(e)); },
    });
  }

  badge(estado: string): string {
    if (estado === 'PROCESADO') return 'ok';
    if (estado === 'FALLIDO') return 'err';
    return 'warn';
  }

  private msg(e: any): string {
    return describeHttpError(e);
  }
}
