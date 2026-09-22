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
    { clave: 'tarifas', nombre: 'Tarifas', servicio: SERVICIOS.tarifas, puerto: 8084 },
    { clave: 'volumetria', nombre: 'Volumetría', servicio: SERVICIOS.volumetria, puerto: 8083 },
    { clave: 'liquidacion-p2p', nombre: 'Liquidación P2P', servicio: SERVICIOS.liquidacionP2P, puerto: 8081 },
    { clave: 'liquidacion-mensual', nombre: 'Liquidación Mensual', servicio: SERVICIOS.liquidacionMensual, puerto: 8082 },
    { clave: 'facturacion', nombre: 'Facturación', servicio: SERVICIOS.facturacion, puerto: 8088 },
    { clave: 'pagos', nombre: 'Pagos', servicio: SERVICIOS.pagos, puerto: 8085 },
  ];

  chequear(servicio: string): Observable<'UP' | 'DOWN'> {
    return this.http.get<{ status: string }>(gw(servicio, 'actuator/health')).pipe(
      map((r) => (r?.status === 'UP' ? 'UP' : 'DOWN')),
      catchError(() => of('DOWN' as const)),
    );
  }
}
