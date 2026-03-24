/** Avoid flooding logs when MySQL is down or misconfigured (interval jobs). */
const lastLogged = new Map()

export function logDbMaintenanceError(label, err) {
  const now = Date.now()
  const prev = lastLogged.get(label) ?? 0
  const intervalMs = 5 * 60 * 1000
  if (now - prev < intervalMs) return
  lastLogged.set(label, now)
  const msg = err?.sqlMessage || err?.message || String(err)
  console.error(`[${label}] ${msg}`)
}
