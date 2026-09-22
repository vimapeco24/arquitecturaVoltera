import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { TarifaEventosService } from '../../services/tarifa-eventos.service';
import { ToastService } from '../../core/toast.service';
import { describeHttpError } from '../../core/http-error';
import { withNums } from '../../core/num';
import { CargoVista, NotificacionLiquidada, SimularConsumoEvento, TarifaEventosStats } from '../../core/models';

@Component({
  selector: 'app-tarifa-eventos',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './tarifa-eventos.component.html',
  styleUrl: './domain-page.scss',
})
export class TarifaEventosComponent {
  private fb = inject(FormBuilder);
  private svc = inject(TarifaEventosService);
  private toast = inject(ToastService);

  cargos: CargoVista[] = [];
  stats?: TarifaEventosStats;
  ultimaNotificacion?: NotificacionLiquidada;
  cargando = false;
  simulando = false;

  formActivar: FormGroup = this.fb.group({
    medidorId: ['MED-EDA-001', Validators.required],
    prosumidorId: ['PRO-EDA-001', Validators.required],
    umbralKwh: [100, [Validators.required, Validators.min(0)]],
  });

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.svc.listar().subscribe({
      next: (r) => { this.cargos = r ?? []; this.cargando = false; },
      error: (e) => { this.cargando = false; this.toast.error('Error al consultar cargos', this.msg(e)); },
    });
    this.svc.stats().subscribe({
      next: (s) => (this.stats = s),
      error: () => (this.stats = undefined),
    });
  }

  /** Simula el consumo de un ConsumoRegistrado (prueba de idempotencia con eventId fijo). */
  simular(extra: boolean): void {
    this.simulando = true;
    const ahora = new Date().toISOString();
    const evento: SimularConsumoEvento = {
      eventId: `EVT-SIM-${Date.now()}`,
      medidorId: 'MED-EDA-SIM',
      prosumidorId: 'PRO-EDA-SIM',
      consumoKwh: extra ? 150 : 80,
      umbralKwh: 100,
      consumoExtra: extra,
      ocurridoEn: ahora,
      emitidoEn: ahora,
    };
    this.svc.simularEvento(evento).subscribe({
      next: (c) => {
        this.simulando = false;
        this.toast.success('Evento simulado', `Cargo ${c.id} · ${c.estado} · ${c.montoCargo} COP`);
        this.cargar();
      },
      error: (e) => { this.simulando = false; this.toast.error('Error al simular evento', this.msg(e)); },
    });
  }

  /** Activa un medidor y genera la notificación liquidada (factura + tarifas). */
  activarMedidor(): void {
    if (this.formActivar.invalid) { this.formActivar.markAllAsTouched(); return; }
    const body = withNums(this.formActivar.value, ['umbralKwh']);
    this.svc.activarMedidor(body).subscribe({
      next: (n) => {
        this.ultimaNotificacion = n;
        this.toast.success('Medidor activado', 'Notificación liquidada generada y publicada al tópico');
      },
      error: (e) => this.toast.error('Error al activar medidor', this.msg(e)),
    });
  }

  badge(estado: string): string {
    if (estado === 'LIQUIDADO') return 'err';
    return 'ok';
  }

  private msg(e: any): string {
    return describeHttpError(e);
  }
}
