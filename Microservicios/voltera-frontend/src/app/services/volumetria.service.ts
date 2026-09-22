import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { gw, SERVICIOS } from '../core/gateway';
import { IngestarLecturaRequest, Lectura } from '../core/models';

@Injectable({ providedIn: 'root' })
export class VolumetriaService {
  constructor(private http: HttpClient) {}

  ingestar(body: IngestarLecturaRequest): Observable<Lectura> {
    return this.http.post<Lectura>(gw(SERVICIOS.volumetria, 'api/v1/lecturas'), body);
  }

  obtener(id: string): Observable<Lectura> {
    return this.http.get<Lectura>(gw(SERVICIOS.volumetria, `api/v1/lecturas/${id}`));
  }

  porMedidor(medidorId: string): Observable<Lectura[]> {
    return this.http.get<Lectura[]>(
      gw(SERVICIOS.volumetria, `api/v1/lecturas?medidorId=${encodeURIComponent(medidorId)}`),
    );
  }

  eliminar(id: string): Observable<void> {
    return this.http.delete<void>(gw(SERVICIOS.volumetria, `api/v1/lecturas/${id}`));
  }

  actualizar(id: string, body: IngestarLecturaRequest): Observable<Lectura> {
    return this.http.put<Lectura>(gw(SERVICIOS.volumetria, `api/v1/lecturas/${id}`), body);
  }
}
