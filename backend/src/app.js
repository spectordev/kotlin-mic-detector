import express from 'express'
import multer from 'multer'
import deviceRouter from './routes/deviceEmail.js'
import recordingsRouter from './routes/recordings.js'

export function createApp() {
  const app = express()
  app.use(express.json({ limit: '64kb' }))

  app.get('/health', (_req, res) => {
    res.json({ ok: true })
  })

  app.use('/device', deviceRouter)
  app.use('/recordings', recordingsRouter)

  app.use((err, _req, res, _next) => {
    console.error(err)
    if (err instanceof multer.MulterError && err.code === 'LIMIT_FILE_SIZE') {
      return res.status(413).json({ error: 'file_too_large' })
    }
    if (err.message === 'Only WAV uploads are allowed') {
      return res.status(400).json({ error: 'invalid_file_type' })
    }
    res.status(500).json({ error: 'internal_error' })
  })

  return app
}
