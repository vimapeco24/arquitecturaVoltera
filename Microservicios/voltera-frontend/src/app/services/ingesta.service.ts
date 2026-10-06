import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { gw, SERVICIOS } from '../core/gateway';

export interface SesionIngesta {
  medidoresConCanal: number;
  reglaMinKwh: number;
  reglaMaxKwh: number;
}

/**
 * Cliente del ingesta-service (BC Telemetría · Ingesta). Abre/cierra canal e inyecta
 * lecturas crudas para validar (regla + ventana de duplicados). El backend emite
 * LecturaValidada / LecturaSospechosaDetectada por el OUTBOX.
 */
@Injectable({ providedIn: 'root' })
export class IngestaService {
  private http = inject(HttpClient);
  private readonly base = 'api/v1/ingesta';

  sesion(): Observable<SesionIngesta> {
    return this.http.get<SesionIngesta>(gw(SERVICIOS.ingesta, `${this.base}/sesion`));
  }
  abrirCanal(serial: string): Observable<any> {
    return this.http.post<any>(gw(SERVICIOS.ingesta, `${this.base}/medidores/${serial}/abrir-canal`), {});
  }
  cerrarCanal(serial: string): Observable<any> {
    return this.http.post<any>(gw(SERVICIOS.ingesta, `${this.base}/medidores/${serial}/cerrar-canal`), {});
  }
  ingestar(medidorSerial: string, consumoKwh: number): Observable<{ medidorSerial: string; resultado: string }> {
    return this.http.post<{ medidorSerial: string; resultado: string }>(
      gw(SERVICIOS.ingesta, `${this.base}/lecturas`),
      { medidorSerial, consumoKwh, capturadaEn: new Date().toISOString() });
  }
  outbox(): Observable<any[]> {
    return this.http.get<any[]>(gw(SERVICIOS.ingesta, `${this.base}/outbox`));
  }
}
