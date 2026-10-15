import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: number;
  type: 'ok' | 'err';
  title: string;
  msg?: string;
  /** Nº de veces que llegó este mismo aviso (agrupado). 1 = una sola vez. */
  count: number;
}

/** Máximo de toasts visibles a la vez (los más viejos se descartan). */
const MAX_VISIBLE = 4;
/** Tiempo de auto-cierre por toast (ms). */
const TTL_MS = 5000;

/**
 * Gestor global de notificaciones tipo toast.
 *
 * Deduplica avisos idénticos: si llega un error con el mismo título+mensaje que
 * uno ya visible (p. ej. varias llamadas que fallan a la vez con "502 timeout
 * WSO2" cuando el gateway está caído), en lugar de apilar N toasts iguales
 * incrementa un contador en el existente y renueva su temporizador. Así se
 * evita la avalancha de toasts cuando el backend no responde.
 */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private seq = 0;
  private timers = new Map<number, ReturnType<typeof setTimeout>>();
  readonly toasts = signal<Toast[]>([]);

  success(title: string, msg?: string): void {
    this.push('ok', title, msg);
  }

  error(title: string, msg?: string): void {
    this.push('err', title, msg);
  }

  private push(type: 'ok' | 'err', title: string, msg?: string): void {
    const existing = this.toasts().find(
      (t) => t.type === type && t.title === title && t.msg === msg,
    );
    if (existing) {
      // Mismo aviso ya visible: agrupar (contador +1) y renovar el temporizador.
      this.toasts.update((list) =>
        list.map((t) => (t.id === existing.id ? { ...t, count: t.count + 1 } : t)),
      );
      this.arm(existing.id);
      return;
    }

    const id = ++this.seq;
    this.toasts.update((list) => {
      const next = [...list, { id, type, title, msg, count: 1 }];
      // Limita la pila: descarta los más viejos si se excede el máximo.
      while (next.length > MAX_VISIBLE) {
        const removed = next.shift();
        if (removed) this.clearTimer(removed.id);
      }
      return next;
    });
    this.arm(id);
  }

  private arm(id: number): void {
    this.clearTimer(id);
    this.timers.set(
      id,
      setTimeout(() => this.dismiss(id), TTL_MS),
    );
  }

  private clearTimer(id: number): void {
    const t = this.timers.get(id);
    if (t) {
      clearTimeout(t);
      this.timers.delete(id);
    }
  }

  dismiss(id: number): void {
    this.clearTimer(id);
    this.toasts.update((list) => list.filter((t) => t.id !== id));
  }
}
