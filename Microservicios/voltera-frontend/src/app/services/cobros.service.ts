import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { gw, SERVICIOS } from '../core/gateway';
import {
  InboxVista,
  MovimientoVista,
  PagoConfirmadoEvento,
  RegistrarMovimientoRequest,
} from '../core/models';

/**
 * Cliente del cobros-service (8100), BC Cobros del dominio seguros.
 *
 * Registra movimientos de cobro de prima y consume (vía broker) el evento
 * PagoConfirmado para conciliarlos emitiendo el recibo. Expone el endpoint
 * simular-pago para probar la conciliación idempotente sin broker.
 */
@Injectable({ providedIn: 'root' })
export class CobrosService {
  constructor(private http: HttpClient) {}

  listar(): Observable<MovimientoVista[]> {
    return this.http.get<MovimientoVista[]>(gw(SERVICIOS.cobros, 'api/v1/movimientos'));
  }

  obtener(id: string): Observable<MovimientoVista> {
    return this.http.get<MovimientoVista>(gw(SERVICIOS.cobros, `api/v1/movimientos/${id}`));
  }

  porPoliza(polizaId: string): Observable<MovimientoVista[]> {
    return this.http.get<MovimientoVista[]>(
      gw(SERVICIOS.cobros, `api/v1/movimientos?polizaId=${encodeURIComponent(polizaId)}`),
    );
  }

  registrar(body: RegistrarMovimientoRequest): Observable<MovimientoVista> {
    return this.http.post<MovimientoVista>(gw(SERVICIOS.cobros, 'api/v1/movimientos'), body);
  }

  /** Inyecta un PagoConfirmado simulado (idempotente por eventId). */
  simularPago(evento: PagoConfirmadoEvento): Observable<MovimientoVista> {
    return this.http.post<MovimientoVista>(
      gw(SERVICIOS.cobros, 'api/v1/movimientos/simular-pago'),
      evento,
    );
  }

  /** Tabla INBOX de idempotencia (únicos/duplicados) del consumer cobros. */
  inbox(): Observable<InboxVista> {
    return this.http.get<InboxVista>(gw(SERVICIOS.cobros, 'api/v1/movimientos/inbox'));
  }
}
