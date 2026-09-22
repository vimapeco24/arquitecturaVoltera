import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { gw, SERVICIOS } from '../core/gateway';
import {
  BufferResponse,
  IngestaResponse,
  IngestarConsumoRequest,
  Medidor,
  RegistrarMedidorRequest,
} from '../core/models';

/**
 * Cliente del telemetria-service (emisor IoT). Registra medidores, los activa y
 * envía lecturas de consumo. Al ingestar una lectura, el backend publica el
 * evento ConsumoRegistrado al broker (o lo encola en el buffer offline).
 */
@Injectable({ providedIn: 'root' })
export class TelemetriaService {
  constructor(private http: HttpClient) {}

  registrar(body: RegistrarMedidorRequest): Observable<Medidor> {
    return this.http.post<Medidor>(gw(SERVICIOS.telemetria, 'api/v1/medidores'), body);
  }

  activar(id: string): Observable<Medidor> {
    return this.http.post<Medidor>(gw(SERVICIOS.telemetria, `api/v1/medidores/${id}/activar`), {});
  }

  suspender(id: string): Observable<Medidor> {
    return this.http.post<Medidor>(gw(SERVICIOS.telemetria, `api/v1/medidores/${id}/suspender`), {});
  }

  ingestar(id: string, body: IngestarConsumoRequest): Observable<IngestaResponse> {
    return this.http.post<IngestaResponse>(
      gw(SERVICIOS.telemetria, `api/v1/medidores/${id}/lecturas`),
      body,
    );
  }

  drenarBuffer(): Observable<BufferResponse> {
    return this.http.post<BufferResponse>(gw(SERVICIOS.telemetria, 'api/v1/medidores/buffer/drenar'), {});
  }

  obtener(id: string): Observable<Medidor> {
    return this.http.get<Medidor>(gw(SERVICIOS.telemetria, `api/v1/medidores/${id}`));
  }

  listar(): Observable<Medidor[]> {
    return this.http.get<Medidor[]>(gw(SERVICIOS.telemetria, 'api/v1/medidores'));
  }
}
