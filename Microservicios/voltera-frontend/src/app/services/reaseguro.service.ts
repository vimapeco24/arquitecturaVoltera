import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { gw, SERVICIOS } from '../core/gateway';
import { CesionVista, ReaseguroStats, SiniestroAprobadoEvento } from '../core/models';

@Injectable({ providedIn: 'root' })
export class ReaseguroService {
  constructor(private http: HttpClient) {}

  listar(): Observable<CesionVista[]> {
    return this.http.get<CesionVista[]>(gw(SERVICIOS.reaseguro, 'api/v1/cesiones'));
  }

  obtener(id: string): Observable<CesionVista> {
    return this.http.get<CesionVista>(gw(SERVICIOS.reaseguro, `api/v1/cesiones/${id}`));
  }

  porPoliza(polizaId: string): Observable<CesionVista[]> {
    return this.http.get<CesionVista[]>(
      gw(SERVICIOS.reaseguro, `api/v1/cesiones?polizaId=${encodeURIComponent(polizaId)}`),
    );
  }

  stats(): Observable<ReaseguroStats> {
    return this.http.get<ReaseguroStats>(gw(SERVICIOS.reaseguro, 'api/v1/cesiones/stats'));
  }

  simularEvento(evento: SiniestroAprobadoEvento): Observable<CesionVista> {
    return this.http.post<CesionVista>(
      gw(SERVICIOS.reaseguro, 'api/v1/cesiones/simular-evento'),
      evento,
    );
  }
}
