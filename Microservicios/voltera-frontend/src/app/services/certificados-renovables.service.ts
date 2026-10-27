import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { gw, SERVICIOS } from '../core/gateway';
import { CertificadoRenovable } from '../core/models';

export interface GatewayEstado {
  tocaLaCadena: boolean;
  usaGateway: boolean;
  gatewayConfigurado: boolean;
  modo: string;
}

/**
 * Cliente del Servicio de Certificados Renovables (8103, lámina 04).
 * NO toca la cadena directamente: solicita al gateway emitir/transferir/retirar
 * el NFT y actualiza su estado tras la confirmación.
 */
@Injectable({ providedIn: 'root' })
export class CertificadosRenovablesService {
  private http = inject(HttpClient);
  private url(path: string): string { return gw(SERVICIOS.certificadosRenovables, path); }

  emitir(body: {
    idMedidor: string; emisor: string; titular: string;
    consumoNetoMensualWh: number; periodo: number; solicitudCertificacionId?: string;
  }): Observable<CertificadoRenovable> {
    return this.http.post<CertificadoRenovable>(this.url('api/v1/certificados-renovables'), body);
  }
  transferir(id: string, hacia: string): Observable<CertificadoRenovable> {
    return this.http.post<CertificadoRenovable>(this.url(`api/v1/certificados-renovables/${id}/transferir`), { hacia });
  }
  retirar(id: string): Observable<CertificadoRenovable> {
    return this.http.post<CertificadoRenovable>(this.url(`api/v1/certificados-renovables/${id}/retirar`), {});
  }
  listar(titular?: string): Observable<CertificadoRenovable[]> {
    const q = titular ? `?titular=${encodeURIComponent(titular)}` : '';
    return this.http.get<CertificadoRenovable[]>(this.url(`api/v1/certificados-renovables${q}`));
  }
  gateway(): Observable<GatewayEstado> {
    return this.http.get<GatewayEstado>(this.url('api/v1/certificados-renovables/gateway'));
  }
}
