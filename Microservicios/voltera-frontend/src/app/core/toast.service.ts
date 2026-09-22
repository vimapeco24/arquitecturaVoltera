import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: number;
  type: 'ok' | 'err';
  title: string;
  msg?: string;
}

/** Gestor global de notificaciones tipo toast. */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private seq = 0;
  readonly toasts = signal<Toast[]>([]);

  success(title: string, msg?: string): void {
    this.push('ok', title, msg);
  }

  error(title: string, msg?: string): void {
    this.push('err', title, msg);
  }

  private push(type: 'ok' | 'err', title: string, msg?: string): void {
    const id = ++this.seq;
    this.toasts.update((list) => [...list, { id, type, title, msg }]);
    setTimeout(() => this.dismiss(id), 5000);
  }

  dismiss(id: number): void {
    this.toasts.update((list) => list.filter((t) => t.id !== id));
  }
}
