import fs from 'fs/promises'
import nodemailer from 'nodemailer'
import { config } from '../config.js'

let transporter

function getTransporter() {
  if (transporter) return transporter
  if (!config.smtp.host) {
    return null
  }
  transporter = nodemailer.createTransport({
    host: config.smtp.host,
    port: config.smtp.port,
    secure: config.smtp.secure,
    auth:
      config.smtp.user && config.smtp.pass
        ? { user: config.smtp.user, pass: config.smtp.pass }
        : undefined,
  })
  return transporter
}

/**
 * Text-only message (no attachment). Same transporter as recording mail — useful for
 * verifying SMTP after a clip is stored in DB, without attaching the file.
 *
 * @returns {{ ok: true } | { ok: false, error: string }}
 */
export async function sendPlainTextEmail({ to, subject, text }) {
  const tx = getTransporter()
  if (!tx) {
    return { ok: false, error: 'SMTP not configured (set SMTP_HOST)' }
  }
  try {
    await tx.sendMail({
      from: config.mailFrom,
      to,
      subject,
      text,
    })
    return { ok: true }
  } catch (e) {
    return { ok: false, error: e.message || String(e) }
  }
}

/**
 * @returns {{ ok: true } | { ok: false, error: string }}
 */
export async function sendRecordingEmail({ to, subject, text, filePath, filename }) {
  const tx = getTransporter()
  if (!tx) {
    return { ok: false, error: 'SMTP not configured (set SMTP_HOST)' }
  }
  try {
    await fs.access(filePath)
  } catch {
    return { ok: false, error: 'Attachment file missing' }
  }
  try {
    await tx.sendMail({
      from: config.mailFrom,
      to,
      subject,
      text,
      attachments: [{ filename, path: filePath }],
    })
    return { ok: true }
  } catch (e) {
    return { ok: false, error: e.message || String(e) }
  }
}
