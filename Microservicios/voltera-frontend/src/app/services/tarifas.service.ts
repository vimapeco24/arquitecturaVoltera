import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { gw, SERVICIOS } from '../core/gateway';
import { CrearTarifaRequest, PrecioHora, Tarifa } from '../core/models';

@Injectable({ providedIn: 'root' })
export class TarifasService {
  constructor(private http: HttpClient) {}

  listar(): Observable<Tarifa[]> {
    return this.http.get<Tarifa[]>(gw(SERVICIOS.tarifas, 'api/v1/tarifas'));
  }

  obtener(id: string): Observable<Tarifa> {
    return this.http.get<Tarifa>(gw(SERVICIOS.tarifas, `api/v1/tarifas/${id}`));
  }

  crear(body: CrearTarifaRequest): Observable<Tarifa> {
    return this.http.post<Tarifa>(gw(SERVICIOS.tarifas, 'api/v1/tarifas'), body);
  }

  precioHora(id: string, hora: number): Observable<PrecioHora> {
    return this.http.get<PrecioHora>(
      gw(SERVICIOS.tarifas, `api/v1/tarifas/${id}/precio?hora=${hora}`),
    );
  }

  eliminar(id: string): Observable<void> {
    return this.http.delete<void>(gw(SERVICIOS.tarifas, `api/v1/tarifas/${id}`));
  }

  actualizar(id: string, body: CrearTarifaRequest): Observable<Tarifa> {
    return this.http.put<Tarifa>(gw(SERVICIOS.tarifas, `api/v1/tarifas/${id}`), body);
  }
}
