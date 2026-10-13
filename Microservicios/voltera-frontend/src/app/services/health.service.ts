import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of } from 'rxjs';
import { gw, SERVICIOS } from '../core/gateway';

export interface ServicioSalud {
  clave: string;
  nombre: string;
  servicio: string;
  puerto: number;
  estado: 'UP' | 'DOWN' | 'CHECKING';
}

/** Consulta el actuator/health de cada microservicio a través del gateway. */
@Injectable({ providedIn: 'root' })
export class HealthService {
  constructor(private http: HttpClient) {}

  readonly catalogo: Omit<ServicioSalud, 'estado'>[] = [
    // Entrega 2 · servicios de negocio REST
    { clave: 'tarifas', nombre: 'Tarifas', servicio: SERVICIOS.tarifas, puerto: 8084 },
    { clave: 'volumetria', nombre: 'Volumetría', servicio: SERVICIOS.volumetria, puerto: 8083 },
    { clave: 'liquidacion-p2p', nombre: 'Liquidación P2P', servicio: SERVICIOS.liquidacionP2P, puerto: 8081 },
    { clave: 'liquidacion-mensual', nombre: 'Liquidación Mensual', servicio: SERVICIOS.liquidacionMensual, puerto: 8082 },
    { clave: 'facturacion', nombre: 'Facturación', servicio: SERVICIOS.facturacion, puerto: 8088 },
    { clave: 'pagos', nombre: 'Pagos', servicio: SERVICIOS.pagos, puerto: 8085 },
    // Entrega 3 · dominio seguros (EDA)
    { clave: 'siniestros', nombre: 'Siniestros', servicio: SERVICIOS.siniestros, puerto: 8091 },
    { clave: 'reaseguro', nombre: 'Reaseguro', servicio: SERVICIOS.reaseguro, puerto: 8092 },
    { clave: 'cobros', nombre: 'Cobros', servicio: SERVICIOS.cobros, puerto: 8100 },
    // Entrega 3 · caso Telemetría (demo EDA)
    { clave: 'telemetria', nombre: 'Telemetría (IoT)', servicio: SERVICIOS.telemetria, puerto: 8093 },
    { clave: 'tarifa-eventos', nombre: 'Tarifa por Eventos', servicio: SERVICIOS.tarifaEventos, puerto: 8094 },
    // Entrega 3 · cadena EDA completa (láminas 02/03/04)
    { clave: 'habilitacion', nombre: 'Habilitación de Medidores', servicio: SERVICIOS.habilitacion, puerto: 8095 },
    { clave: 'integracion-ami', nombre: 'Integración AMI (ACL)', servicio: SERVICIOS.integracionAmi, puerto: 8096 },
    { clave: 'ingesta', nombre: 'Ingesta de Telemetría', servicio: SERVICIOS.ingesta, puerto: 8097 },
    { clave: 'telemetria-core', nombre: 'Telemetría Core · CQRS', servicio: SERVICIOS.telemetriaCore, puerto: 8098 },
    { clave: 'notificaciones', nombre: 'Notificaciones', servicio: SERVICIOS.notificaciones, puerto: 8099 },
  ];

  chequear(servicio: string): Observable<'UP' | 'DOWN'> {
    return this.http.get<{ status: string }>(gw(servicio, 'actuator/health')).pipe(
      map((r) => (r?.status === 'UP' ? 'UP' : 'DOWN')),
      catchError(() => of('DOWN' as const)),
    );
  }
}
