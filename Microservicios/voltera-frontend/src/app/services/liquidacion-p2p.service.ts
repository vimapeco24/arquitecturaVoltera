import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { gw, SERVICIOS } from '../core/gateway';
import { EmparejarRequest, TransaccionP2P } from '../core/models';

@Injectable({ providedIn: 'root' })
export class LiquidacionP2pService {
  constructor(private http: HttpClient) {}

  emparejar(body: EmparejarRequest): Observable<TransaccionP2P> {
    return this.http.post<TransaccionP2P>(
      gw(SERVICIOS.liquidacionP2P, 'api/v1/transacciones/emparejar'),
      body,
    );
  }

  obtener(id: string): Observable<TransaccionP2P> {
    return this.http.get<TransaccionP2P>(
      gw(SERVICIOS.liquidacionP2P, `api/v1/transacciones/${id}`),
    );
  }

  porProsumidor(prosumidorId: string): Observable<TransaccionP2P[]> {
    return this.http.get<TransaccionP2P[]>(
      gw(SERVICIOS.liquidacionP2P, `api/v1/transacciones?prosumidorId=${encodeURIComponent(prosumidorId)}`),
    );
  }

  eliminar(id: string): Observable<void> {
    return this.http.delete<void>(gw(SERVICIOS.liquidacionP2P, `api/v1/transacciones/${id}`));
  }

  actualizar(id: string, body: EmparejarRequest): Observable<TransaccionP2P> {
    return this.http.put<TransaccionP2P>(
      gw(SERVICIOS.liquidacionP2P, `api/v1/transacciones/${id}`),
      body,
    );
  }
}
