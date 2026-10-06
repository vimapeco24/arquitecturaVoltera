import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { gw, SERVICIOS } from '../core/gateway';

export interface ConexionAmi {
  proveedorAmi: string;
  protocolo: string;
  estado: string;
  medidoresAtendidos: number;
}

export interface LecturaCrudaDto {
  medidorSerial: string;
  valor: number;
  unidad: string;
  capturadaEn?: string;
}

/**
 * Cliente del integracion-ami-service (ACL / Adaptador de proveedores). Recibe lotes
 * de lecturas crudas del head-end, gestiona medidores atendidos y reporta degradación.
 */
@Injectable({ providedIn: 'root' })
export class IntegracionAmiService {
  private http = inject(HttpClient);
  private readonly base = 'api/v1/ami';

  conexion(): Observable<ConexionAmi> {
    return this.http.get<ConexionAmi>(gw(SERVICIOS.integracionAmi, `${this.base}/conexion`));
  }
  recibirLote(proveedorAmi: string, lecturas: LecturaCrudaDto[]): Observable<any> {
    return this.http.post<any>(gw(SERVICIOS.integracionAmi, `${this.base}/lecturas-crudas`),
      { proveedorAmi, lecturas });
  }
  habilitar(serial: string): Observable<any> {
    return this.http.post<any>(gw(SERVICIOS.integracionAmi, `${this.base}/medidores/${serial}/habilitar`), {});
  }
  suspender(serial: string): Observable<any> {
    return this.http.post<any>(gw(SERVICIOS.integracionAmi, `${this.base}/medidores/${serial}/suspender`), {});
  }
  degradar(motivo: string): Observable<any> {
    return this.http.post<any>(gw(SERVICIOS.integracionAmi, `${this.base}/degradar?motivo=${encodeURIComponent(motivo)}`), {});
  }
  outbox(): Observable<any[]> {
    return this.http.get<any[]>(gw(SERVICIOS.integracionAmi, `${this.base}/outbox`));
  }
}
