/**
 * Utilidades para asegurar que los valores numéricos se envían al backend con
 * PUNTO decimal (2.5) y no con coma (2,5), independientemente del locale del
 * navegador. Un <input type="number"> en locale es-ES puede entregar "2,5",
 * que rompe la deserialización JSON del backend.
 */

/** Convierte a número con punto decimal, aceptando coma o punto. */
export function toNum(v: unknown): number {
  if (typeof v === 'number') return v;
  if (v === null || v === undefined || v === '') return NaN;
  const n = Number(String(v).trim().replace(',', '.'));
  return n;
}

/** Devuelve una copia del objeto con las claves indicadas convertidas a número (punto). */
export function withNums<T extends Record<string, any>>(obj: T, keys: (keyof T)[]): T {
  const out: any = { ...obj };
  for (const k of keys) {
    if (out[k] !== undefined && out[k] !== null && out[k] !== '') {
      const n = toNum(out[k]);
      if (!Number.isNaN(n)) out[k] = n;
    }
  }
  return out;
}

/**
 * Formatea una fecha como hora LOCAL 'YYYY-MM-DDTHH:mm' para <input type="datetime-local">.
 * Evita el bug de usar toISOString() (UTC) como valor local, que desplazaba la
 * hora al futuro y hacía que el backend marcara la lectura como RECHAZADA.
 */
export function toLocalInput(d: Date = new Date()): string {
  const local = new Date(d.getTime() - d.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 16);
}
