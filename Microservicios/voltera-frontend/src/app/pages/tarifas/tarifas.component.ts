import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { TarifasService } from '../../services/tarifas.service';
import { ToastService } from '../../core/toast.service';
import { describeHttpError } from '../../core/http-error';
import { toNum } from '../../core/num';
import { Tarifa } from '../../core/models';

@Component({
  selector: 'app-tarifas',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './tarifas.component.html',
  styleUrl: './domain-page.scss',
})
export class TarifasComponent implements OnInit {
  private fb = inject(FormBuilder);
  private svc = inject(TarifasService);
  private toast = inject(ToastService);

  tarifas: Tarifa[] = [];
  cargando = false;
  enviando = false;
  seleccion?: Tarifa;
  horaConsulta = 19;
  precioConsultado?: number;
  editandoId?: string;

  form: FormGroup = this.fb.group({
    nombre: ['Residencial', Validators.required],
    franjas: this.fb.array([
      this.nuevaFranja(0, 18, 500),
      this.nuevaFranja(18, 22, 800),
      this.nuevaFranja(22, 24, 500),
    ]),
  });

  get franjas(): FormArray {
    return this.form.get('franjas') as FormArray;
  }

  ngOnInit(): void {
    this.listar();
  }

  nuevaFranja(hi = 0, hf = 1, precio = 0): FormGroup {
    return this.fb.group({
      horaInicio: [hi, [Validators.required, Validators.min(0), Validators.max(23)]],
      horaFin: [hf, [Validators.required, Validators.min(1), Validators.max(24)]],
      precioKwh: [precio, [Validators.required, Validators.min(0)]],
    });
  }

  agregarFranja(): void {
    this.franjas.push(this.nuevaFranja());
  }

  quitarFranja(i: number): void {
    if (this.franjas.length > 1) this.franjas.removeAt(i);
  }

  listar(): void {
    this.cargando = true;
    this.svc.listar().subscribe({
      next: (r) => { this.tarifas = r ?? []; this.cargando = false; },
      error: (e) => { this.cargando = false; this.toast.error('No se pudo listar tarifas', this.msg(e)); },
    });
  }

  crear(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.enviando = true;
    const body = {
      nombre: this.form.value.nombre,
      franjas: (this.form.value.franjas ?? []).map((f: any) => ({
        horaInicio: toNum(f.horaInicio),
        horaFin: toNum(f.horaFin),
        precioKwh: toNum(f.precioKwh),
      })),
    };
    if (this.editandoId) {
      const id = this.editandoId;
      this.svc.actualizar(id, body).subscribe({
        next: (t) => {
          this.enviando = false;
          this.toast.success('Actualizado', `${t.nombre} · ${t.franjas.length} franjas`);
          this.cancelarEdicion();
          this.listar();
        },
        error: (e) => { this.enviando = false; this.toast.error('Error al actualizar tarifa', this.msg(e)); },
      });
      return;
    }
    this.svc.crear(body).subscribe({
      next: (t) => {
        this.enviando = false;
        this.toast.success('Tarifa creada', `${t.nombre} · ${t.franjas.length} franjas`);
        this.listar();
      },
      error: (e) => { this.enviando = false; this.toast.error('Error al crear tarifa', this.msg(e)); },
    });
  }

  editar(t: Tarifa): void {
    this.editandoId = t.id;
    this.form.reset();
    this.franjas.clear();
    (t.franjas ?? []).forEach((f) =>
      this.franjas.push(this.nuevaFranja(f.horaInicio, f.horaFin, f.precioKwh)),
    );
    if (this.franjas.length === 0) this.franjas.push(this.nuevaFranja());
    this.form.patchValue({ nombre: t.nombre });
    setTimeout(() => document.querySelector('form.panel')?.scrollIntoView({ behavior: 'smooth', block: 'start' }));
  }

  cancelarEdicion(): void {
    this.editandoId = undefined;
    this.form.reset();
    this.franjas.clear();
    this.franjas.push(this.nuevaFranja(0, 18, 500));
    this.franjas.push(this.nuevaFranja(18, 22, 800));
    this.franjas.push(this.nuevaFranja(22, 24, 500));
    this.form.patchValue({ nombre: 'Residencial' });
  }

  eliminar(t: Tarifa): void {
    if (!window.confirm('¿Eliminar este registro? Esta acción no se puede deshacer.')) return;
    this.svc.eliminar(t.id).subscribe({
      next: () => { this.toast.success('Eliminado'); this.listar(); },
      error: (e) => this.toast.error('Error al eliminar', this.msg(e)),
    });
  }

  seleccionar(t: Tarifa): void {
    this.seleccion = t;
    this.precioConsultado = undefined;
  }

  consultarPrecio(): void {
    if (!this.seleccion) return;
    this.svc.precioHora(this.seleccion.id, this.horaConsulta).subscribe({
      next: (p) => { this.precioConsultado = p.precioKwh; },
      error: (e) => this.toast.error('Error al consultar precio', this.msg(e)),
    });
  }

  private msg(e: any): string {
    return describeHttpError(e);
  }
}
