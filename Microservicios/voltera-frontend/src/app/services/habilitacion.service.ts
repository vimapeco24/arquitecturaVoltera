import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { gw, SERVICIOS } from '../core/gateway';

export interface MedidorHab {
  medidorId: string;
  serial: string;
  estado: string;
  canalIngestaCreado?: boolean;
  tarifaAsignada?: boolean;
  motivoFallo?: string | null;
}

export interface HabilitarRequest {
  medidorId: string;
  serial: string;
  fabricante?: string;
  codigoPunto: string;
  direccion?: string;
  ordenInstalacionId?: string;
}

/**
 * Cliente del habilitacion-service (BC Habilitación / "Serv. de Transferencia de
 * Estado"). Dispara el alta (OrdenInstalacionCerrada), confirma CanalIngestaCreado /
 * TarifaAsignada, fuerza timeout y consulta estado + outbox.
 */
@Injectable({ providedIn: 'root' })
export class HabilitacionService {
  private http = inject(HttpClient);
  private readonly base = 'api/v1/habilitacion';

  habilitar(body: HabilitarRequest): Observable<MedidorHab> {
    return this.http.post<MedidorHab>(gw(SERVICIOS.habilitacion, `${this.base}/medidores`), body);
  }
  canalIngesta(id: string): Observable<MedidorHab> {
    return this.http.post<MedidorHab>(gw(SERVICIOS.habilitacion, `${this.base}/medidores/${id}/canal-ingesta-creado`), {});
  }
  tarifaAsignada(id: string): Observable<MedidorHab> {
    return this.http.post<MedidorHab>(gw(SERVICIOS.habilitacion, `${this.base}/medidores/${id}/tarifa-asignada`), {});
  }
  forzarTimeout(id: string): Observable<MedidorHab> {
    return this.http.post<MedidorHab>(gw(SERVICIOS.habilitacion, `${this.base}/medidores/${id}/forzar-timeout`), {});
  }
  listar(): Observable<MedidorHab[]> {
    return this.http.get<MedidorHab[]>(gw(SERVICIOS.habilitacion, `${this.base}/medidores`));
  }
  outbox(): Observable<any[]> {
    return this.http.get<any[]>(gw(SERVICIOS.habilitacion, `${this.base}/outbox`));
  }
}
