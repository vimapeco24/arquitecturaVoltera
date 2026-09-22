import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { HealthService, ServicioSalud } from '../../services/health.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit {
  private healthSvc = inject(HealthService);

  servicios: ServicioSalud[] = [];
  cargando = true;
  chequeando = false;

  readonly flujo = [
    { n: 1, t: 'Tarifas', d: 'Define el precio por franja horaria', ruta: '/tarifas', ic: '⚡' },
    { n: 2, t: 'Volumetría', d: 'Ingesta y valida lecturas de medidores', ruta: '/volumetria', ic: '📊' },
    { n: 3, t: 'Liquidación P2P', d: 'Empareja compra/venta en tiempo real', ruta: '/liquidacion-p2p', ic: '🔁' },
    { n: 4, t: 'Liquidación Mensual', d: 'Totaliza consumos y excedentes', ruta: '/liquidacion-mensual', ic: '🗓️' },
    { n: 5, t: 'Facturación', d: 'Genera la factura mensual del prosumidor', ruta: '/facturacion', ic: '🧾' },
    { n: 6, t: 'Pagos', d: 'Cobra la factura de forma idempotente', ruta: '/pagos', ic: '💳' },
  ];

  ngOnInit(): void {
    this.servicios = this.healthSvc.catalogo.map((c) => ({ ...c, estado: 'CHECKING' }));
    this.refrescar();
  }

  refrescar(): void {
    this.chequeando = true;
    this.servicios = this.servicios.map((s) => ({ ...s, estado: 'CHECKING' }));
    forkJoin(this.servicios.map((s) => this.healthSvc.chequear(s.servicio))).subscribe({
      next: (estados) => {
        this.servicios = this.servicios.map((s, i) => ({ ...s, estado: estados[i] }));
        this.cargando = false;
        this.chequeando = false;
      },
      error: () => {
        this.servicios = this.servicios.map((s) => ({ ...s, estado: 'DOWN' }));
        this.cargando = false;
        this.chequeando = false;
      },
    });
  }

  get activos(): number {
    return this.servicios.filter((s) => s.estado === 'UP').length;
  }
}
