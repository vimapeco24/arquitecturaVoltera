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
  cobros: 'cobros',
  telemetria: 'telemetria',
  tarifaEventos: 'tarifa-eventos',
  // Cadena EDA del laboratorio (láminas 02/03/04/10)
  habilitacion: 'habilitacion',
  integracionAmi: 'integracion-ami',
  ingesta: 'ingesta',
  telemetriaCore: 'telemetria-core',
  notificaciones: 'notificaciones',
  // Servicio de borde Blockchain (anclaje de liquidaciones P2P en Polygon Amoy)
  blockchain: 'blockchain',
  // Nuevos microservicios DDD alrededor de la cadena (lámina 04). NO tocan la cadena.
  certificacionGeneracion: 'certificacion-generacion',
  certificadosRenovables: 'certificados-renovables',
} as const;
