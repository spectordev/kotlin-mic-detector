import fs from 'fs/promises'
import path from 'path'
import { config } from '../config.js'
import { query } from '../db.js'

/**
 * Deletes DB rows and files for old recordings (sent/failed) past retention.
 */
export async function cleanupOldRecordings() {
  const hours = config.cleanupRecordingAgeHours
  const rows = await query(
    `SELECT id, stored_path FROM recordings
     WHERE status IN ('sent', 'failed')
       AND created_at < DATE_SUB(NOW(), INTERVAL ? HOUR)`,
    [hours],
  )
  for (const row of rows) {
    const absPath = path.isAbsolute(row.stored_path)
      ? row.stored_path
      : path.resolve(row.stored_path)
    try {
      await fs.unlink(absPath)
    } catch {
      /* ignore */
    }
    await query(`DELETE FROM recordings WHERE id = ?`, [row.id])
  }
  return rows.length
}
