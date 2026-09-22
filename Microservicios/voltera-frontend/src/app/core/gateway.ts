import { environment } from '../../environments/environment';

/**
 * Construye la URL completa hacia un servicio a través del API Gateway WSO2.
 * Patrón: {gatewayBase}/{servicio}/v1/{path}
 * Ejemplo: gw('tarifas', 'api/v1/tarifas') -> /gw/tarifas/v1/api/v1/tarifas
 */
export function gw(servicio: string, path: string): string {
  const clean = path.startsWith('/') ? path.slice(1) : path;
  return `${environment.gatewayBase}/${servicio}/v1/${clean}`;
}

/** Nombres de servicio tal como están publicados en el gateway. */
export const SERVICIOS = {
  tarifas: 'tarifas',
  volumetria: 'volumetria',
  liquidacionP2P: 'liquidacion-p2p',
  liquidacionMensual: 'liquidacion-mensual',
  facturacion: 'facturacion',
  pagos: 'pagos',
  siniestros: 'siniestros',
  reaseguro: 'reaseguro',
  telemetria: 'telemetria',
  tarifaEventos: 'tarifa-eventos',
} as const;
