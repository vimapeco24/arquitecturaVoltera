import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'panel' },
  {
    path: 'panel',
    title: 'Panel · Voltera',
    loadComponent: () => import('./pages/dashboard/dashboard.component').then((m) => m.DashboardComponent),
  },
  {
    path: 'tarifas',
    title: 'Tarifas · Voltera',
    loadComponent: () => import('./pages/tarifas/tarifas.component').then((m) => m.TarifasComponent),
  },
  {
    path: 'volumetria',
    title: 'Volumetría · Voltera',
    loadComponent: () => import('./pages/volumetria/volumetria.component').then((m) => m.VolumetriaComponent),
  },
  {
    path: 'liquidacion-p2p',
    title: 'Liquidación P2P · Voltera',
    loadComponent: () => import('./pages/liquidacion-p2p/liquidacion-p2p.component').then((m) => m.LiquidacionP2pComponent),
  },
  {
    path: 'liquidacion-mensual',
    title: 'Liquidación Mensual · Voltera',
    loadComponent: () => import('./pages/liquidacion-mensual/liquidacion-mensual.component').then((m) => m.LiquidacionMensualComponent),
  },
  {
    path: 'facturacion',
    title: 'Facturación · Voltera',
    loadComponent: () => import('./pages/facturacion/facturacion.component').then((m) => m.FacturacionComponent),
  },
  {
    path: 'pagos',
    title: 'Pagos · Voltera',
    loadComponent: () => import('./pages/pagos/pagos.component').then((m) => m.PagosComponent),
  },
  {
    path: 'siniestros',
    title: 'Siniestros · Voltera',
    loadComponent: () => import('./pages/siniestros/siniestros.component').then((m) => m.SiniestrosComponent),
  },
  {
    path: 'reaseguro',
    title: 'Reaseguro · Voltera',
    loadComponent: () => import('./pages/reaseguro/reaseguro.component').then((m) => m.ReaseguroComponent),
  },
  {
    path: 'telemetria',
    title: 'Telemetría · Voltera',
    loadComponent: () => import('./pages/telemetria/telemetria.component').then((m) => m.TelemetriaComponent),
  },
  {
    path: 'tarifa-eventos',
    title: 'Tarifa por Eventos · Voltera',
    loadComponent: () => import('./pages/tarifa-eventos/tarifa-eventos.component').then((m) => m.TarifaEventosComponent),
  },
  {
    path: 'orquestacion',
    title: 'Orquestación · Outbox · Inbox · Voltera',
    loadComponent: () => import('./pages/orquestacion/orquestacion.component').then((m) => m.OrquestacionComponent),
  },
  {
    path: 'experimentos-telemetria',
    title: 'Telemetría · Experimentos y Patrones · Voltera',
    loadComponent: () => import('./pages/experimentos-telemetria/experimentos-telemetria.component').then((m) => m.ExperimentosTelemetriaComponent),
  },
  {
    path: 'lab-metricas',
    title: 'Laboratorio · Métricas · Voltera',
    loadComponent: () => import('./pages/lab-metricas/lab-metricas.component').then((m) => m.LabMetricasComponent),
  },
  {
    path: 'patrones',
    title: 'Patrones & Tecnología · Voltera',
    loadComponent: () => import('./pages/patrones/patrones.component').then((m) => m.PatronesComponent),
  },
  {
    path: 'experimentos',
    title: 'Experimentos · Voltera',
    loadComponent: () => import('./pages/experimentos/experimentos.component').then((m) => m.ExperimentosComponent),
  },
  {
    path: 'observabilidad',
    title: 'Observabilidad · Voltera',
    loadComponent: () => import('./pages/observabilidad/observabilidad.component').then((m) => m.ObservabilidadComponent),
  },
  { path: '**', redirectTo: 'panel' },
];
