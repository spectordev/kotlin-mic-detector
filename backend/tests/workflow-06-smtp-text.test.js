/**
 * Workflow 3 — Delivery: plain-text SMTP (no attachment), after upload/DB success path.
 *
 * Requires: SMTP_* + MAIL_FROM + SMTP_TEST_TO in backend/.env
 * Skipped when SMTP_HOST or SMTP_TEST_TO is missing.
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { sendPlainTextEmail } from '../src/services/emailService.js'

const smtpConfigured = Boolean(process.env.SMTP_HOST?.trim())
const testInbox = process.env.SMTP_TEST_TO?.trim()
const shouldSkip = !smtpConfigured || !testInbox

test(
  'SMTP delivers plain text from MAIL_FROM to SMTP_TEST_TO (no attachment)',
  { skip: shouldSkip },
  async () => {
    const result = await sendPlainTextEmail({
      to: testInbox,
      subject: '[BgApp backend test] Plain text (no recording attachment)',
      text: [
        'This is an automated test message.',
        '',
        'Scenario: as if a recording was already uploaded and persisted in the database;',
        'this checks only the text-mail path (no attachment).',
        '',
        `Sent at: ${new Date().toISOString()}`,
      ].join('\n'),
    })

    assert.equal(result.ok, true, result.ok === false ? result.error : '')
  },
)
