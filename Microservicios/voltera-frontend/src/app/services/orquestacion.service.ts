import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { gw, SERVICIOS } from '../core/gateway';
import { IniciarAltaRequest, InboxVista, OutboxMensaje, SagaAlta } from '../core/models';

/**
 * Cliente del patrón Orquestación (mediador) del tarifa-eventos-service (lámina 10).
 * Dirige la saga de alta de medidor y expone los patrones de soporte OUTBOX e INBOX.
 */
@Injectable({ providedIn: 'root' })
export class OrquestacionService {
  private readonly base = 'api/v1/orquestacion';
  constructor(private http: HttpClient) {}

  iniciarAlta(body: IniciarAltaRequest): Observable<SagaAlta> {
    return this.http.post<SagaAlta>(gw(SERVICIOS.tarifaEventos, `${this.base}/saga/alta`), body);
  }

  canalIngestaCreado(sagaId: string): Observable<SagaAlta> {
    return this.http.post<SagaAlta>(
      gw(SERVICIOS.tarifaEventos, `${this.base}/saga/${sagaId}/canal-ingesta-creado`), {});
  }

  tarifaAsignada(sagaId: string): Observable<SagaAlta> {
    return this.http.post<SagaAlta>(
      gw(SERVICIOS.tarifaEventos, `${this.base}/saga/${sagaId}/tarifa-asignada`), {});
  }

  forzarTimeout(sagaId: string): Observable<SagaAlta> {
    return this.http.post<SagaAlta>(
      gw(SERVICIOS.tarifaEventos, `${this.base}/saga/${sagaId}/forzar-timeout`), {});
  }

  listarSagas(): Observable<SagaAlta[]> {
    return this.http.get<SagaAlta[]>(gw(SERVICIOS.tarifaEventos, `${this.base}/saga`));
  }

  outbox(): Observable<OutboxMensaje[]> {
    return this.http.get<OutboxMensaje[]>(gw(SERVICIOS.tarifaEventos, `${this.base}/saga/outbox`));
  }

  inbox(): Observable<InboxVista> {
    return this.http.get<InboxVista>(gw(SERVICIOS.tarifaEventos, `${this.base}/saga/inbox`));
  }
}
