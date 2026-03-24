import { Router } from 'express'
import { query } from '../db.js'
import {
  deviceEmailBodySchema,
  deviceEmailQuerySchema,
  validateBody,
} from '../middleware/validate.js'

const router = Router()

router.post('/email', validateBody(deviceEmailBodySchema), async (req, res, next) => {
  try {
    const { device_id, email } = req.validated
    await query(
      `INSERT INTO devices (id, email) VALUES (?, ?)
       ON DUPLICATE KEY UPDATE email = VALUES(email), updated_at = CURRENT_TIMESTAMP`,
      [device_id, email],
    )
    res.status(200).json({ ok: true, device_id })
  } catch (e) {
    next(e)
  }
})

router.get('/email', async (req, res, next) => {
  try {
    const parsed = deviceEmailQuerySchema.safeParse(req.query)
    if (!parsed.success) {
      return res.status(400).json({ error: 'validation_error', details: parsed.error.flatten() })
    }
    const { device_id } = parsed.data
    const rows = await query('SELECT email, updated_at FROM devices WHERE id = ? LIMIT 1', [
      device_id,
    ])
    if (!rows.length) {
      return res.status(404).json({ error: 'not_found' })
    }
    res.json({ device_id, email: rows[0].email, updated_at: rows[0].updated_at })
  } catch (e) {
    next(e)
  }
})

export default router
