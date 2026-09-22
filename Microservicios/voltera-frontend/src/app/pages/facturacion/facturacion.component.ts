import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { FacturacionService } from '../../services/facturacion.service';
import { ToastService } from '../../core/toast.service';
import { describeHttpError } from '../../core/http-error';
import { withNums } from '../../core/num';
import { Factura } from '../../core/models';

@Component({
  selector: 'app-facturacion',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './facturacion.component.html',
  styleUrl: './domain-page.scss',
})
export class FacturacionComponent {
  private fb = inject(FormBuilder);
  private svc = inject(FacturacionService);
  private toast = inject(ToastService);

  facturas: Factura[] = [];
  ultima?: Factura;
  enviando = false;
  cargando = false;
  filtroProsumidor = 'PRO-001';
  editandoId?: string;

  form: FormGroup = this.fb.group({
    prosumidorId: ['PRO-001', Validators.required],
    periodoInicio: ['2026-01-01', Validators.required],
    periodoFin: ['2026-01-31', Validators.required],
    kwhConsumidos: [100, [Validators.required, Validators.min(0)]],
    kwhInyectados: [30, [Validators.required, Validators.min(0)]],
    precioConsumoKwh: [680, [Validators.required, Validators.min(0)]],
    precioExcedenteKwh: [420, [Validators.required, Validators.min(0)]],
    // Opcional: dispara la llamada real facturación -> tarifas (salto entre micros)
    tarifaId: [''],
    hora: [0, [Validators.min(0), Validators.max(23)]],
  });

  emitir(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.enviando = true;
    const body = withNums(this.form.value, ['kwhConsumidos', 'kwhInyectados', 'precioConsumoKwh', 'precioExcedenteKwh', 'hora']);
    if (this.editandoId) {
      const id = this.editandoId;
      this.svc.actualizar(id, body).subscribe({
        next: (f) => {
          this.enviando = false;
          this.ultima = f;
          this.toast.success('Actualizado', `Total ${f.total} COP`);
          this.filtroProsumidor = f.prosumidorId;
          this.cancelarEdicion();
          this.buscar();
        },
        error: (e) => { this.enviando = false; this.toast.error('Error al actualizar', this.msg(e)); },
      });
      return;
    }
    this.svc.emitir(body).subscribe({
      next: (f) => {
        this.enviando = false;
        this.ultima = f;
        this.toast.success('Factura emitida', `Total ${f.total} COP`);
        this.filtroProsumidor = f.prosumidorId;
        this.buscar();
      },
      error: (e) => { this.enviando = false; this.toast.error('Error al emitir', this.msg(e)); },
    });
  }

  editar(f: Factura): void {
    this.editandoId = f.id;
    this.form.patchValue({
      prosumidorId: f.prosumidorId,
      periodoInicio: (f.periodoInicio ?? '').slice(0, 10),
      periodoFin: (f.periodoFin ?? '').slice(0, 10),
      kwhConsumidos: f.kwhConsumidos,
      kwhInyectados: f.kwhInyectados,
    });
    setTimeout(() => document.querySelector('form.panel')?.scrollIntoView({ behavior: 'smooth', block: 'start' }));
  }

  cancelarEdicion(): void {
    this.editandoId = undefined;
    this.form.reset({
      prosumidorId: 'PRO-001',
      periodoInicio: '2026-01-01',
      periodoFin: '2026-01-31',
      kwhConsumidos: 100,
      kwhInyectados: 30,
      precioConsumoKwh: 680,
      precioExcedenteKwh: 420,
      tarifaId: '',
      hora: 0,
    });
  }

  eliminar(f: Factura): void {
    if (!window.confirm('¿Eliminar este registro? Esta acción no se puede deshacer.')) return;
    this.svc.eliminar(f.id).subscribe({
      next: () => { this.toast.success('Eliminado'); this.buscar(); },
      error: (e) => this.toast.error('Error al eliminar', this.msg(e)),
    });
  }

  buscar(): void {
    if (!this.filtroProsumidor) return;
    this.cargando = true;
    this.svc.porProsumidor(this.filtroProsumidor).subscribe({
      next: (r) => { this.facturas = r ?? []; this.cargando = false; },
      error: (e) => { this.cargando = false; this.toast.error('Error al consultar', this.msg(e)); },
    });
  }

  badge(estado: string): string {
    if (estado === 'PAGADA') return 'ok';
    if (estado === 'ANULADA') return 'err';
    return 'info';
  }

  private msg(e: any): string {
    return describeHttpError(e);
  }
}
