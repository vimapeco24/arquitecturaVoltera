import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { gw, SERVICIOS } from '../core/gateway';
import { Pago, ProcesarPagoRequest } from '../core/models';

@Injectable({ providedIn: 'root' })
export class PagosService {
  constructor(private http: HttpClient) {}

  procesar(body: ProcesarPagoRequest): Observable<Pago> {
    return this.http.post<Pago>(gw(SERVICIOS.pagos, 'api/v1/pagos'), body);
  }

  obtener(id: string): Observable<Pago> {
    return this.http.get<Pago>(gw(SERVICIOS.pagos, `api/v1/pagos/${id}`));
  }

  porProsumidor(prosumidorId: string): Observable<Pago[]> {
    return this.http.get<Pago[]>(
      gw(SERVICIOS.pagos, `api/v1/pagos?prosumidorId=${encodeURIComponent(prosumidorId)}`),
    );
  }

  eliminar(id: string): Observable<void> {
    return this.http.delete<void>(gw(SERVICIOS.pagos, `api/v1/pagos/${id}`));
  }

  actualizar(id: string, body: ProcesarPagoRequest): Observable<Pago> {
    return this.http.put<Pago>(gw(SERVICIOS.pagos, `api/v1/pagos/${id}`), body);
  }
}
