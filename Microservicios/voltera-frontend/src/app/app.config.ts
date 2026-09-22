import { ApplicationConfig, LOCALE_ID, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { registerLocaleData } from '@angular/common';
import localeEnUs from '@angular/common/locales/en';

import { routes } from './app.routes';
import { authInterceptor } from './core/auth.interceptor';

// Se usa el locale en-US para que los números muestren PUNTO como separador
// decimal (p. ej. 2.5, no 2,5) de forma consistente sin importar la
// configuración regional del navegador.
registerLocaleData(localeEnUs);

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withInMemoryScrolling({ scrollPositionRestoration: 'top' })),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideAnimations(),
    { provide: LOCALE_ID, useValue: 'en-US' },
  ],
};
