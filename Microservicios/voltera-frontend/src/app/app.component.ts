import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { ToastService } from './core/toast.service';

interface NavItem {
  path: string;
  label: string;
  icon: string;
  step?: number;
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  readonly toastSvc = inject(ToastService);
  collapsed = false;

  readonly nav: NavItem[] = [
    { path: '/panel', label: 'Panel', icon: '◎' },
    { path: '/tarifas', label: 'Tarifas', icon: '⚡', step: 1 },
    { path: '/volumetria', label: 'Volumetría', icon: '📊', step: 2 },
    { path: '/liquidacion-p2p', label: 'Liquidación P2P', icon: '🔁', step: 3 },
    { path: '/liquidacion-mensual', label: 'Liquidación Mensual', icon: '🗓️', step: 4 },
    { path: '/facturacion', label: 'Facturación', icon: '🧾', step: 5 },
    { path: '/pagos', label: 'Pagos', icon: '💳', step: 6 },
    { path: '/siniestros', label: 'Siniestros', icon: '🔥' },
    { path: '/reaseguro', label: 'Reaseguro', icon: '🛡️' },
    { path: '/telemetria', label: 'Telemetría', icon: '📡' },
    { path: '/tarifa-eventos', label: 'Tarifa Eventos', icon: '💰' },
    { path: '/experimentos-telemetria', label: 'Exp. Telemetría', icon: '🧪' },
    { path: '/patrones', label: 'Patrones & Tec.', icon: '📚' },
    { path: '/lab-metricas', label: 'Lab · Métricas', icon: '🔬' },
    { path: '/experimentos', label: 'Experimentos', icon: '🧪' },
    { path: '/observabilidad', label: 'Observabilidad', icon: '🔭' },
  ];

  toggle(): void {
    this.collapsed = !this.collapsed;
  }
}
