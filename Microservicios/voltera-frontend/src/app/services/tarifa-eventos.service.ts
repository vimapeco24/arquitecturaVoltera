import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { gw, SERVICIOS } from '../core/gateway';
import {
  ActivarMedidorRequest,
  CargoVista,
  NotificacionLiquidada,
  SimularConsumoEvento,
  TarifaEventosStats,
} from '../core/models';

/**
 * Cliente del tarifa-eventos-service (consumidor CQRS). Las consultas leen del
 * lado de LECTURA (vista materializada). Permite simular eventos (idempotencia)
 * y activar medidores (notificación liquidada).
 */
@Injectable({ providedIn: 'root' })
export class TarifaEventosService {
  constructor(private http: HttpClient) {}

  listar(): Observable<CargoVista[]> {
    return this.http.get<CargoVista[]>(gw(SERVICIOS.tarifaEventos, 'api/v1/cargos'));
  }

  obtener(id: string): Observable<CargoVista> {
    return this.http.get<CargoVista>(gw(SERVICIOS.tarifaEventos, `api/v1/cargos/${id}`));
  }

  porProsumidor(prosumidorId: string): Observable<CargoVista[]> {
    return this.http.get<CargoVista[]>(
      gw(SERVICIOS.tarifaEventos, `api/v1/cargos?prosumidorId=${encodeURIComponent(prosumidorId)}`),
    );
  }

  stats(): Observable<TarifaEventosStats> {
    return this.http.get<TarifaEventosStats>(gw(SERVICIOS.tarifaEventos, 'api/v1/cargos/stats'));
  }

  simularEvento(evento: SimularConsumoEvento): Observable<CargoVista> {
    return this.http.post<CargoVista>(
      gw(SERVICIOS.tarifaEventos, 'api/v1/cargos/simular-evento'),
      evento,
    );
  }

  activarMedidor(body: ActivarMedidorRequest): Observable<NotificacionLiquidada> {
    return this.http.post<NotificacionLiquidada>(
      gw(SERVICIOS.tarifaEventos, 'api/v1/cargos/activar-medidor'),
      body,
    );
  }
}
