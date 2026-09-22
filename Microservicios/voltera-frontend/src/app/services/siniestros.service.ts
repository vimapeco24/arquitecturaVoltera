import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { gw, SERVICIOS } from '../core/gateway';
import { ReportarSiniestroRequest, Siniestro } from '../core/models';

@Injectable({ providedIn: 'root' })
export class SiniestrosService {
  constructor(private http: HttpClient) {}

  reportar(body: ReportarSiniestroRequest): Observable<Siniestro> {
    return this.http.post<Siniestro>(gw(SERVICIOS.siniestros, 'api/v1/siniestros'), body);
  }

  aprobar(id: string): Observable<Siniestro> {
    return this.http.post<Siniestro>(gw(SERVICIOS.siniestros, `api/v1/siniestros/${id}/aprobar`), {});
  }

  obtener(id: string): Observable<Siniestro> {
    return this.http.get<Siniestro>(gw(SERVICIOS.siniestros, `api/v1/siniestros/${id}`));
  }

  listar(): Observable<Siniestro[]> {
    return this.http.get<Siniestro[]>(gw(SERVICIOS.siniestros, 'api/v1/siniestros'));
  }

  porPoliza(polizaId: string): Observable<Siniestro[]> {
    return this.http.get<Siniestro[]>(
      gw(SERVICIOS.siniestros, `api/v1/siniestros?polizaId=${encodeURIComponent(polizaId)}`),
    );
  }
}
