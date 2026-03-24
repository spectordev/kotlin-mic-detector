/**
 * Sanity check: backend .env path and MySQL password visibility (masked).
 * Run from backend folder: npm run check-env
 */
import dotenv from 'dotenv'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const root = path.join(__dirname, '..', '..')
const envPath = path.join(root, '.env')

dotenv.config({ path: envPath })
dotenv.config()

const pw = process.env.MYSQL_PASSWORD
const masked =
  pw === undefined
    ? '(unset)'
    : pw.length === 0
      ? '(empty — causes MySQL "using password: NO")'
      : `${'*'.repeat(Math.min(pw.length, 8))} (${pw.length} chars)`

console.log('Backend root:', root)
console.log('Loaded .env from:', envPath)
console.log('MYSQL_USER:', process.env.MYSQL_USER || '(default bgapp)')
console.log('MYSQL_PASSWORD:', masked)
console.log('MYSQL_DATABASE:', process.env.MYSQL_DATABASE || '(default bgapp)')
console.log('MYSQL_HOST:', process.env.MYSQL_HOST || '(default 127.0.0.1)')
console.log('')
if (pw === undefined || pw.length === 0) {
  console.warn('Set MYSQL_PASSWORD in backend/.env (must match bootstrap-mysql.sql).')
}
if (!process.env.SMTP_HOST) {
  console.warn('SMTP_HOST is empty — clips may upload but email will not send until SMTP is configured.')
}
