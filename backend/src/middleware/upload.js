import fs from 'fs'
import multer from 'multer'
import path from 'path'
import { config } from '../config.js'

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

export function createUploadMiddleware() {
  ensureDir(config.uploadDir)
  const storage = multer.diskStorage({
    destination: (_req, _file, cb) => {
      cb(null, config.uploadDir)
    },
    filename: (_req, file, cb) => {
      const safe = file.originalname.replace(/[^a-zA-Z0-9._-]/g, '_')
      const name = `${Date.now()}_${safe}`
      cb(null, name)
    },
  })
  return multer({
    storage,
    limits: { fileSize: config.maxFileBytes },
    fileFilter: (_req, file, cb) => {
      const ok =
        file.mimetype === 'audio/wav' ||
        file.mimetype === 'audio/x-wav' ||
        file.originalname.toLowerCase().endsWith('.wav')
      if (!ok) {
        return cb(new Error('Only WAV uploads are allowed'))
      }
      cb(null, true)
    },
  })
}

export function recordingUploadFields(upload) {
  return upload.fields([
    { name: 'recipient_email', maxCount: 1 },
    { name: 'audio', maxCount: 1 },
  ])
}
