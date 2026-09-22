import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of, shareReplay, tap, map, catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';

interface TokenResponse {
  access_token: string;
  token_type: string;
  expires_in: number;
  scope?: string;
}

/**
 * Servicio de autenticación OAuth2 (grant_type=client_credentials) contra WSO2.
 * Cachea el token en memoria y lo renueva automáticamente cuando expira.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private token: string | null = null;
  private expiresAt = 0;
  private inFlight$?: Observable<string>;

  constructor(private http: HttpClient) {}

  /** Devuelve un token válido, reutilizando el cacheado o solicitando uno nuevo. */
  getToken(): Observable<string> {
    const now = Date.now();
    if (this.token && now < this.expiresAt - 30_000) {
      return of(this.token);
    }
    if (this.inFlight$) {
      return this.inFlight$;
    }
    this.inFlight$ = this.requestToken().pipe(
      tap((t) => {
        this.inFlight$ = undefined;
        return t;
      }),
      shareReplay(1),
    );
    return this.inFlight$;
  }

  private requestToken(): Observable<string> {
    // Modo BFF (producción en Vercel): el endpoint /api/token custodia el
    // consumerSecret del lado servidor; el navegador NO envía Basic.
    const isBff = environment.tokenUrl.startsWith('/api');

    let headers = new HttpHeaders({
      'Content-Type': 'application/x-www-form-urlencoded',
    });
    if (!isBff) {
      const basic = btoa(
        `${environment.oauth.consumerKey}:${environment.oauth.consumerSecret}`,
      );
      headers = headers.set('Authorization', `Basic ${basic}`);
    }
    const body = new URLSearchParams();
    body.set('grant_type', 'client_credentials');

    return this.http
      .post<TokenResponse>(environment.tokenUrl, body.toString(), { headers })
      .pipe(
        map((res) => {
          this.token = res.access_token;
          this.expiresAt = Date.now() + (res.expires_in ?? 3600) * 1000;
          return this.token;
        }),
        catchError((err) => {
          this.inFlight$ = undefined;
          return throwError(() => err);
        }),
      );
  }

  /** Estado de sesión para la UI. */
  get isAuthenticated(): boolean {
    return !!this.token && Date.now() < this.expiresAt;
  }

  get tokenExpiryDate(): Date | null {
    return this.expiresAt ? new Date(this.expiresAt) : null;
  }

  /** Fuerza limpiar el token cacheado. */
  clear(): void {
    this.token = null;
    this.expiresAt = 0;
    this.inFlight$ = undefined;
  }
}
