import { Router } from 'express'
import { v4 as uuidv4 } from 'uuid'
import { z } from 'zod'
import { query } from '../db.js'
import { createUploadMiddleware, recordingUploadFields } from '../middleware/upload.js'
import { scheduleRecordingDelivery } from '../services/recordingProcessor.js'

const router = Router()
const upload = createUploadMiddleware()
const uploadMiddleware = recordingUploadFields(upload)

const recipientSchema = z.string().trim().email().max(320)

router.post('/upload', uploadMiddleware, async (req, res, next) => {
  try {
    const rawEmail = req.body?.recipient_email
    const recipient_email = typeof rawEmail === 'string' ? rawEmail : rawEmail?.[0]
    const parsedEmail = recipientSchema.safeParse(recipient_email)
    if (!parsedEmail.success) {
      return res.status(400).json({
        error: 'invalid_recipient_email',
        details: parsedEmail.error.flatten(),
      })
    }

    const file = req.files?.audio?.[0]
    if (!file) {
      return res.status(400).json({ error: 'audio_file_required' })
    }

    const deviceIdRaw = req.body?.device_id
    const device_id =
      typeof deviceIdRaw === 'string' ? deviceIdRaw.trim().slice(0, 64) || null : null

    const id = uuidv4()
    await query(
      `INSERT INTO recordings (
        id, device_id, recipient_email, original_filename, stored_path, mime_type, file_size, status
      ) VALUES (?, ?, ?, ?, ?, ?, ?, 'pending')`,
      [
        id,
        device_id,
        parsedEmail.data,
        file.originalname || 'clip.wav',
        file.path,
        file.mimetype || 'audio/wav',
        file.size,
      ],
    )

    scheduleRecordingDelivery(id)

    res.status(201).json({
      id,
      status: 'pending',
      message: 'Upload accepted; delivery is asynchronous.',
    })
  } catch (e) {
    next(e)
  }
})

export default router
