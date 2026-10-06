import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { gw, SERVICIOS } from '../core/gateway';

export interface Notificacion {
  notificacionId: string;
  medidorId: string;
  tipo: string;
  canal: string;
  mensaje: string;
  generadaEn: string;
}

/**
 * Cliente del notificaciones-service (consumidor puro). Historial de notificaciones
 * y endpoints de simulación que inyectan los eventos de entrada sin broker.
 */
@Injectable({ providedIn: 'root' })
export class NotificacionesService {
  private http = inject(HttpClient);
  private readonly base = 'api/v1/notificaciones';

  historial(): Observable<Notificacion[]> {
    return this.http.get<Notificacion[]>(gw(SERVICIOS.notificaciones, this.base));
  }
  simularHabilitado(medidorId: string, serial: string): Observable<Notificacion> {
    return this.http.post<Notificacion>(gw(SERVICIOS.notificaciones, `${this.base}/simular/medidor-habilitado`),
      { medidorId, serial });
  }
  simularSospechosa(medidorSerial: string, consumoKwh: number, motivo: string): Observable<Notificacion> {
    return this.http.post<Notificacion>(gw(SERVICIOS.notificaciones, `${this.base}/simular/lectura-sospechosa`),
      { medidorSerial, consumoKwh, motivo });
  }
  simularSinReporte(medidorSerial: string, ultimaLecturaEn: string): Observable<Notificacion> {
    return this.http.post<Notificacion>(gw(SERVICIOS.notificaciones, `${this.base}/simular/medidor-sin-reporte`),
      { medidorSerial, ultimaLecturaEn });
  }
  simularFactura(medidorSerial: string, facturaId: string, periodo: string, monto: number): Observable<Notificacion> {
    return this.http.post<Notificacion>(gw(SERVICIOS.notificaciones, `${this.base}/simular/factura-emitida`),
      { medidorSerial, facturaId, periodo, monto });
  }
  outbox(): Observable<any[]> {
    return this.http.get<any[]>(gw(SERVICIOS.notificaciones, `${this.base}/outbox`));
  }
}
