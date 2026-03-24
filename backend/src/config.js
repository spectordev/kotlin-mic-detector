import dotenv from 'dotenv'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
/** Parent of `src/` — load `.env` here so credentials work even if cwd is not `backend/` */
const root = path.join(__dirname, '..')

dotenv.config({ path: path.join(root, '.env') })
dotenv.config()

export const config = {
  port: Number(process.env.PORT) || 3000,
  nodeEnv: process.env.NODE_ENV || 'development',
  mysql: {
    host: process.env.MYSQL_HOST || '127.0.0.1',
    port: Number(process.env.MYSQL_PORT) || 3306,
    user: process.env.MYSQL_USER || 'bgapp',
    password: process.env.MYSQL_PASSWORD || '',
    database: process.env.MYSQL_DATABASE || 'bgapp',
  },
  uploadDir: path.resolve(process.env.UPLOAD_DIR || path.join(root, 'data', 'uploads')),
  maxFileBytes: Number(process.env.MAX_FILE_BYTES) || 12 * 1024 * 1024,
  smtp: {
    host: process.env.SMTP_HOST || '',
    port: Number(process.env.SMTP_PORT) || 587,
    secure: process.env.SMTP_SECURE === 'true',
    user: process.env.SMTP_USER || '',
    pass: process.env.SMTP_PASS || '',
  },
  mailFrom: process.env.MAIL_FROM || 'BgApp <noreply@localhost>',
  emailMaxAttempts: Number(process.env.EMAIL_MAX_ATTEMPTS) || 5,
  emailRetryBaseMs: Number(process.env.EMAIL_RETRY_BASE_MS) || 5000,
  cleanupRecordingAgeHours: Number(process.env.CLEANUP_RECORDING_AGE_HOURS) || 168,
  cleanupIntervalMs: Number(process.env.CLEANUP_INTERVAL_MS) || 60 * 60 * 1000,
}
