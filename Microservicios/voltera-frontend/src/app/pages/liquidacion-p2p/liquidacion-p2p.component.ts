import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { LiquidacionP2pService } from '../../services/liquidacion-p2p.service';
import { ToastService } from '../../core/toast.service';
import { describeHttpError } from '../../core/http-error';
import { withNums } from '../../core/num';
import { TransaccionP2P } from '../../core/models';

@Component({
  selector: 'app-liquidacion-p2p',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './liquidacion-p2p.component.html',
  styleUrl: './domain-page.scss',
})
export class LiquidacionP2pComponent {
  private fb = inject(FormBuilder);
  private svc = inject(LiquidacionP2pService);
  private toast = inject(ToastService);

  transacciones: TransaccionP2P[] = [];
  ultima?: TransaccionP2P;
  enviando = false;
  cargando = false;
  filtroProsumidor = 'VENDEDOR';
  editandoId?: string;

  form: FormGroup = this.fb.group({
    vendedorId: ['VENDEDOR', Validators.required],
    kwhVenta: [10, [Validators.required, Validators.min(0)]],
    precioVenta: [400, [Validators.required, Validators.min(0)]],
    compradorId: ['COMPRADOR', Validators.required],
    kwhCompra: [10, [Validators.required, Validators.min(0)]],
    precioCompra: [500, [Validators.required, Validators.min(0)]],
  });

  emparejar(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.enviando = true;
    const body = withNums(this.form.value, ['kwhVenta', 'precioVenta', 'kwhCompra', 'precioCompra']);
    if (this.editandoId) {
      const id = this.editandoId;
      this.svc.actualizar(id, body).subscribe({
        next: (t) => {
          this.enviando = false;
          this.ultima = t;
          this.toast.success('Actualizado', `${t.energiaKwh} kWh a ${t.precioCasacionKwh} COP/kWh`);
          this.filtroProsumidor = t.vendedorId;
          this.cancelarEdicion();
          this.buscar();
        },
        error: (e) => { this.enviando = false; this.toast.error('Error al actualizar', this.msg(e)); },
      });
      return;
    }
    this.svc.emparejar(body).subscribe({
      next: (t) => {
        this.enviando = false;
        this.ultima = t;
        this.toast.success('Transacción casada', `${t.energiaKwh} kWh a ${t.precioCasacionKwh} COP/kWh`);
        this.filtroProsumidor = t.vendedorId;
        this.buscar();
      },
      error: (e) => { this.enviando = false; this.toast.error('Error al emparejar', this.msg(e)); },
    });
  }

  editar(t: TransaccionP2P): void {
    this.editandoId = t.id;
    this.form.patchValue({
      vendedorId: t.vendedorId,
      kwhVenta: t.energiaSolicitadaVentaKwh,
      precioVenta: t.precioCasacionKwh,
      compradorId: t.compradorId,
      kwhCompra: t.energiaSolicitadaCompraKwh,
      precioCompra: t.precioCasacionKwh,
    });
    setTimeout(() => document.querySelector('form.panel')?.scrollIntoView({ behavior: 'smooth', block: 'start' }));
  }

  cancelarEdicion(): void {
    this.editandoId = undefined;
    this.form.reset({
      vendedorId: 'VENDEDOR',
      kwhVenta: 10,
      precioVenta: 400,
      compradorId: 'COMPRADOR',
      kwhCompra: 10,
      precioCompra: 500,
    });
  }

  eliminar(t: TransaccionP2P): void {
    if (!window.confirm('¿Eliminar este registro? Esta acción no se puede deshacer.')) return;
    this.svc.eliminar(t.id).subscribe({
      next: () => { this.toast.success('Eliminado'); this.buscar(); },
      error: (e) => this.toast.error('Error al eliminar', this.msg(e)),
    });
  }

  buscar(): void {
    if (!this.filtroProsumidor) return;
    this.cargando = true;
    this.svc.porProsumidor(this.filtroProsumidor).subscribe({
      next: (r) => { this.transacciones = r ?? []; this.cargando = false; },
      error: (e) => { this.cargando = false; this.toast.error('Error al consultar', this.msg(e)); },
    });
  }

  private msg(e: any): string {
    return describeHttpError(e);
  }
}
