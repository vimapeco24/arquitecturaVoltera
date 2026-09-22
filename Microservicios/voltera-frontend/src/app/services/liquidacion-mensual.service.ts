import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { gw, SERVICIOS } from '../core/gateway';
import { AbrirLiquidacionRequest, LiquidacionMensual, MovimientoRequest } from '../core/models';

@Injectable({ providedIn: 'root' })
export class LiquidacionMensualService {
  constructor(private http: HttpClient) {}

  abrir(body: AbrirLiquidacionRequest): Observable<LiquidacionMensual> {
    return this.http.post<LiquidacionMensual>(
      gw(SERVICIOS.liquidacionMensual, 'api/v1/liquidaciones'),
      body,
    );
  }

  registrarMovimiento(id: string, body: MovimientoRequest): Observable<LiquidacionMensual> {
    return this.http.post<LiquidacionMensual>(
      gw(SERVICIOS.liquidacionMensual, `api/v1/liquidaciones/${id}/movimientos`),
      body,
    );
  }

  cerrar(id: string): Observable<LiquidacionMensual> {
    return this.http.post<LiquidacionMensual>(
      gw(SERVICIOS.liquidacionMensual, `api/v1/liquidaciones/${id}/cerrar`),
      {},
    );
  }

  obtener(id: string): Observable<LiquidacionMensual> {
    return this.http.get<LiquidacionMensual>(
      gw(SERVICIOS.liquidacionMensual, `api/v1/liquidaciones/${id}`),
    );
  }

  porProsumidor(prosumidorId: string): Observable<LiquidacionMensual[]> {
    return this.http.get<LiquidacionMensual[]>(
      gw(SERVICIOS.liquidacionMensual, `api/v1/liquidaciones?prosumidorId=${encodeURIComponent(prosumidorId)}`),
    );
  }

  eliminar(id: string): Observable<void> {
    return this.http.delete<void>(gw(SERVICIOS.liquidacionMensual, `api/v1/liquidaciones/${id}`));
  }

  actualizar(id: string, body: AbrirLiquidacionRequest): Observable<LiquidacionMensual> {
    return this.http.put<LiquidacionMensual>(
      gw(SERVICIOS.liquidacionMensual, `api/v1/liquidaciones/${id}`),
      body,
    );
  }
}
