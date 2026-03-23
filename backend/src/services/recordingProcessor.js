import fs from 'fs/promises'
import path from 'path'
import { config } from '../config.js'
import { query } from '../db.js'
import { sendRecordingEmail } from './emailService.js'

const inFlight = new Set()

async function loadRecording(id) {
  const rows = await query(
    `SELECT id, recipient_email, original_filename, stored_path, status, attempts
     FROM recordings WHERE id = ? LIMIT 1`,
    [id],
  )
  return rows[0] || null
}

function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms))
}

export async function scheduleRecordingDelivery(recordingId) {
  setImmediate(() => processRecordingWithRetries(recordingId))
}

async function processRecordingWithRetries(recordingId) {
  if (inFlight.has(recordingId)) return
  inFlight.add(recordingId)
  try {
    for (;;) {
      const row = await loadRecording(recordingId)
      if (!row || row.status === 'sent') return

      if (row.attempts >= config.emailMaxAttempts) {
        await query(`UPDATE recordings SET status = 'failed' WHERE id = ?`, [recordingId])
        return
      }

      await query(`UPDATE recordings SET status = 'sending' WHERE id = ?`, [recordingId])

      const absPath = path.isAbsolute(row.stored_path)
        ? row.stored_path
        : path.resolve(row.stored_path)

      const result = await sendRecordingEmail({
        to: row.recipient_email,
        subject: `Voice clip: ${row.original_filename}`,
        text: 'Attached recording from BgApp.',
        filePath: absPath,
        filename: row.original_filename,
      })

      await query(
        `UPDATE recordings SET attempts = attempts + 1, last_error = ? WHERE id = ?`,
        [result.ok ? null : result.error, recordingId],
      )

      if (result.ok) {
        await query(
          `UPDATE recordings SET status = 'sent', sent_at = NOW() WHERE id = ?`,
          [recordingId],
        )
        try {
          await fs.unlink(absPath)
        } catch {
          /* ignore */
        }
        return
      }

      const updated = await loadRecording(recordingId)
      if (!updated || updated.attempts >= config.emailMaxAttempts) {
        await query(`UPDATE recordings SET status = 'failed' WHERE id = ?`, [recordingId])
        return
      }

      await query(`UPDATE recordings SET status = 'pending' WHERE id = ?`, [recordingId])
      const delay = Math.min(
        config.emailRetryBaseMs * Math.pow(2, updated.attempts - 1),
        120_000,
      )
      await sleep(delay)
    }
  } finally {
    inFlight.delete(recordingId)
  }
}

/** Phase 4: re-queue failed deliveries that still have attempts left */
export async function retryStaleRecordings() {
  const rows = await query(
    `SELECT id FROM recordings
     WHERE status = 'failed' AND attempts < ?
     ORDER BY created_at ASC
     LIMIT 25`,
    [config.emailMaxAttempts],
  )
  for (const r of rows) {
    await query(`UPDATE recordings SET status = 'pending' WHERE id = ?`, [r.id])
    scheduleRecordingDelivery(r.id)
  }
}
