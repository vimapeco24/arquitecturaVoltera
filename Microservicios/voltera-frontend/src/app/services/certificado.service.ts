import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { gw, SERVICIOS } from '../core/gateway';
import { CertificadoNft } from '../core/models';

/** Cuerpo para emitir un certificado NFT. */
export interface EmitirCertificadoBody {
  emisor: string;
  propietario: string;
  idMedidor: string;
  consumoNetoMensualWh: number;
  periodo: number;
}

/** Info del contrato NFT que expone el gateway. */
export interface NftRedInfo {
  red: string;
  chainId: number;
  contrato: string;
  tipo: string;
  disponible: boolean;
}

/**
 * Cliente del gateway blockchain para los Certificados de Origen NFT (ERC-721).
 * Mismas reglas de resolución de URL que BlockchainService (directo a Render o
 * vía WSO2). Endpoints: /api/v1/certificados.
 */
@Injectable({ providedIn: 'root' })
export class CertificadoService {
  private http = inject(HttpClient);

  private url(path: string): string {
    const base = environment.blockchainBase?.trim();
    if (base) {
      const clean = path.startsWith('/') ? path.slice(1) : path;
      return `${base.replace(/\/$/, '')}/${clean}`;
    }
    return gw(SERVICIOS.blockchain, path);
  }

  emitir(body: EmitirCertificadoBody): Observable<CertificadoNft> {
    return this.http.post<CertificadoNft>(this.url('api/v1/certificados'), body);
  }

  transferir(tokenId: number, hacia: string): Observable<CertificadoNft> {
    return this.http.post<CertificadoNft>(this.url(`api/v1/certificados/${tokenId}/transferir`), { hacia });
  }

  quemar(tokenId: number): Observable<CertificadoNft> {
    return this.http.post<CertificadoNft>(this.url(`api/v1/certificados/${tokenId}/quemar`), {});
  }

  obtener(tokenId: number): Observable<CertificadoNft> {
    return this.http.get<CertificadoNft>(this.url(`api/v1/certificados/${tokenId}`));
  }

  listar(propietario?: string): Observable<CertificadoNft[]> {
    const q = propietario ? `?propietario=${encodeURIComponent(propietario)}` : '';
    return this.http.get<CertificadoNft[]>(this.url(`api/v1/certificados${q}`));
  }

  red(): Observable<NftRedInfo> {
    return this.http.get<NftRedInfo>(this.url('api/v1/certificados/red'));
  }
}
