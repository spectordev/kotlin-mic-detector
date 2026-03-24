/**
 * Workflow 2 — Recording upload HTTP: client errors without successful DB insert.
 * Full upload + 201: set RUN_INTEGRATION_TESTS=1 and MYSQL_* + schema applied.
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import request from 'supertest'
import { createApp } from '../src/app.js'

const app = createApp()
const integration = process.env.RUN_INTEGRATION_TESTS === '1'

/** Multer allows .wav by extension; content is irrelevant for these HTTP tests */
function tinyWav(name = 'clip.wav') {
  return { buffer: Buffer.from('RIFFtest'), filename: name }
}

test('POST /recordings/upload returns 400 when recipient_email invalid', async () => {
  const { buffer, filename } = tinyWav()
  const res = await request(app)
    .post('/recordings/upload')
    .field('recipient_email', 'not-an-email')
    .attach('audio', buffer, filename)
    .expect(400)
  assert.equal(res.body.error, 'invalid_recipient_email')
})

test('POST /recordings/upload returns 400 when audio file missing', async () => {
  const res = await request(app)
    .post('/recordings/upload')
    .field('recipient_email', 'user@example.com')
    .expect(400)
  assert.equal(res.body.error, 'audio_file_required')
})

test('POST /recordings/upload returns 400 for non-wav attachment', async () => {
  const res = await request(app)
    .post('/recordings/upload')
    .field('recipient_email', 'user@example.com')
    .attach('audio', Buffer.from('hello'), 'note.txt')
    .expect(400)
  assert.equal(res.body.error, 'invalid_file_type')
})

test('POST /recordings/upload returns 201 and id when DB available', { skip: !integration }, async () => {
  const { buffer, filename } = tinyWav(`clip-${Date.now()}.wav`)
  const res = await request(app)
    .post('/recordings/upload')
    .field('recipient_email', 'upload-test@example.com')
    .field('device_id', 'integration-device')
    .attach('audio', buffer, filename)
    .expect(201)
  assert.ok(res.body.id)
  assert.equal(res.body.status, 'pending')
})
