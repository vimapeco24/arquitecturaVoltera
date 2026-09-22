import { HttpErrorResponse } from '@angular/common/http';

/**
 * Traduce un error HTTP a un mensaje claro para el usuario, distinguiendo los
 * fallos típicos al consumir el gateway WSO2 desde un frontend público:
 *  - status 0  -> el navegador no pudo alcanzar el gateway. Casi siempre es
 *                 CORS, certificado TLS no confiable, o el gateway caído.
 *  - 401       -> falta token o token inválido (OAuth2).
 *  - 403       -> token válido pero sin permiso/suscripción.
 *  - 404       -> ruta no publicada en el gateway.
 *  - 5xx       -> error del microservicio backend.
 */
export function describeHttpError(e: unknown): string {
  if (e instanceof HttpErrorResponse) {
    if (e.status === 0) {
      return 'No se pudo contactar el gateway. Revisa que el servidor esté arriba y que el CORS y el certificado TLS de atiesia.synology.me sean válidos.';
    }
    if (e.status === 401) {
      return 'No autorizado (401). El token OAuth2 falta o expiró; revisa el endpoint del token (:3001/oauth2/token).';
    }
    if (e.status === 403) {
      return 'Prohibido (403). El token es válido pero la aplicación no tiene permiso/suscripción sobre esta API.';
    }
    if (e.status === 404) {
      return 'Ruta no encontrada (404). Verifica que la API esté publicada y desplegada en el gateway.';
    }
    if (e.status >= 500) {
      return `Error del servicio backend (${e.status}). ${extractMsg(e) ?? ''}`.trim();
    }
    return extractMsg(e) ?? `HTTP ${e.status}`;
  }
  return (e as { message?: string })?.message ?? 'Error desconocido';
}

function extractMsg(e: HttpErrorResponse): string | null {
  const body = e.error;
  if (!body) return null;
  if (typeof body === 'string') return body;
  return body.message ?? body.description ?? body.error ?? null;
}
