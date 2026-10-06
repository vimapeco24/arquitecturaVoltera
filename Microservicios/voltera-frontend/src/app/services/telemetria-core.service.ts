import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { gw, SERVICIOS } from '../core/gateway';

export interface ConsumoVista {
  id: string;
  medidorSerial: string;
  inicioIntervalo: string;
  finIntervalo: string;
  consumoNetoKwh: number;
  numLecturas: number;
  registradoEn: string;
}

export interface ConsumoAgregado {
  medidorSerial: string;
  consumoTotalKwh: number;
  intervalosRegistrados: number;
  ultimoIntervaloFin: string | null;
  actualizadoEn: string;
}

/**
 * Cliente del telemetria-core-service (CQRS, lámina 03). Lado QUERY: consulta la
 * vista por intervalo y la vista AGREGADA (materializadas por el Proyector).
 * Incluye el endpoint de simulación de LecturaValidada (lado command).
 */
@Injectable({ providedIn: 'root' })
export class TelemetriaCoreService {
  private http = inject(HttpClient);
  private readonly base = 'api/v1/telemetria-core';

  consumo(medidorSerial?: string): Observable<ConsumoVista[]> {
    const q = medidorSerial ? `?medidorSerial=${encodeURIComponent(medidorSerial)}` : '';
    return this.http.get<ConsumoVista[]>(gw(SERVICIOS.telemetriaCore, `${this.base}/consumo${q}`));
  }
  consumoAgregado(medidorSerial?: string): Observable<ConsumoAgregado | ConsumoAgregado[]> {
    const q = medidorSerial ? `?medidorSerial=${encodeURIComponent(medidorSerial)}` : '';
    return this.http.get<ConsumoAgregado | ConsumoAgregado[]>(gw(SERVICIOS.telemetriaCore, `${this.base}/consumo-agregado${q}`));
  }
  simularLectura(medidorSerial: string, consumoKwh: number): Observable<any> {
    return this.http.post<any>(gw(SERVICIOS.telemetriaCore, `${this.base}/simular-lectura`),
      { medidorSerial, consumoKwh, capturadaEn: new Date().toISOString() });
  }
  outbox(): Observable<any[]> {
    return this.http.get<any[]>(gw(SERVICIOS.telemetriaCore, `${this.base}/outbox`));
  }
}
