import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { gw, SERVICIOS } from '../core/gateway';
import { BalanceGeneracion, SolicitudCertificacion } from '../core/models';

/**
 * Cliente del Servicio de Certificación de Generación (8102, lámina 04).
 * NO toca la cadena: registra generación de Telemetría, reserva bloques de
 * 1 MWh sin reutilizar mediciones y reúne los dos avales.
 */
@Injectable({ providedIn: 'root' })
export class CertificacionGeneracionService {
  private http = inject(HttpClient);
  private url(path: string): string { return gw(SERVICIOS.certificacionGeneracion, path); }

  registrarGeneracion(body: { medidorId: string; medicionId: string; wh: number }): Observable<BalanceGeneracion> {
    return this.http.post<BalanceGeneracion>(this.url('api/v1/generacion'), body);
  }
  balances(): Observable<BalanceGeneracion[]> {
    return this.http.get<BalanceGeneracion[]>(this.url('api/v1/balances'));
  }
  solicitar(body: { medidorId: string; propietario: string; periodo: number }): Observable<SolicitudCertificacion> {
    return this.http.post<SolicitudCertificacion>(this.url('api/v1/solicitudes'), body);
  }
  avalarVoltera(id: string): Observable<SolicitudCertificacion> {
    return this.http.post<SolicitudCertificacion>(this.url(`api/v1/solicitudes/${id}/aval-voltera`), {});
  }
  avalarCertificadora(id: string): Observable<SolicitudCertificacion> {
    return this.http.post<SolicitudCertificacion>(this.url(`api/v1/solicitudes/${id}/aval-certificadora`), {});
  }
  rechazar(id: string, motivo: string): Observable<SolicitudCertificacion> {
    return this.http.post<SolicitudCertificacion>(this.url(`api/v1/solicitudes/${id}/rechazar`), { motivo });
  }
  solicitudes(): Observable<SolicitudCertificacion[]> {
    return this.http.get<SolicitudCertificacion[]>(this.url('api/v1/solicitudes'));
  }
}
