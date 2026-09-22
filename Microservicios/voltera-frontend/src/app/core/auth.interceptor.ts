import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { switchMap } from 'rxjs';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

/**
 * Interceptor funcional: para toda petición dirigida al API Gateway, obtiene un
 * token OAuth2 válido y lo inyecta como cabecera `Authorization: Bearer`.
 * No intercepta la petición del propio token (evita bucles).
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);

  const isTokenRequest =
    req.url.includes('/oauth2/token') ||
    req.url === environment.tokenUrl ||
    req.url.startsWith('/api/token');
  const isGateway = req.url.startsWith(environment.gatewayBase);

  if (isTokenRequest || !isGateway) {
    return next(req);
  }

  return auth.getToken().pipe(
    switchMap((token) => {
      const authorized = req.clone({
        setHeaders: { Authorization: `Bearer ${token}` },
      });
      return next(authorized);
    }),
  );
};
