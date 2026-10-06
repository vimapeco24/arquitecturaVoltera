import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ConexionAmi, IntegracionAmiService } from '../../services/integracion-ami.service';
import { ToastService } from '../../core/toast.service';
import { describeHttpError } from '../../core/http-error';

/**
 * Página "Integración AMI" (ACL / Adaptador de proveedores). Recibe lotes de lecturas
 * crudas del head-end y las traduce al formato canónico (emite LecturaCrudaRecibida).
 */
@Component({
  selector: 'app-integracion-ami',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './integracion-ami.component.html',
  styleUrl: './domain-page.scss',
})
export class IntegracionAmiComponent {
  private fb = inject(FormBuilder);
  private svc = inject(IntegracionAmiService);
  private toast = inject(ToastService);

  conexionAmi?: ConexionAmi;
  ultimo?: any;

  form: FormGroup = this.fb.group({
    medidorSerial: ['SER-001', Validators.required],
    valor: [45, [Validators.required, Validators.min(0)]],
    unidad: ['kWh', Validators.required],
  });

  ngOnInit(): void { this.cargarConexion(); }

  cargarConexion(): void {
    this.svc.conexion().subscribe({
      next: (c) => (this.conexionAmi = c),
      error: (e) => this.toast.error('Error al consultar conexión', this.msg(e)),
    });
  }
  enviarLote(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.value;
    this.svc.recibirLote(this.conexionAmi?.proveedorAmi ?? 'Landis+Gyr', [
      { medidorSerial: v.medidorSerial, valor: Number(v.valor), unidad: v.unidad, capturadaEn: new Date().toISOString() },
    ]).subscribe({
      next: (r) => { this.ultimo = r; this.toast.success('Lote recibido', `canónicas: ${r.emitidasCanonicas}`); this.cargarConexion(); },
      error: (e) => this.toast.error('Error al enviar lote', this.msg(e)),
    });
  }
  degradar(): void {
    this.svc.degradar('Fallo simulado del head-end').subscribe({
      next: (r) => { this.toast.success('Proveedor degradado', `${r.proveedorAmi} · ${r.estado}`); this.cargarConexion(); },
      error: (e) => this.toast.error('Error al degradar', this.msg(e)),
    });
  }
  private msg(e: any): string { return describeHttpError(e); }
}
