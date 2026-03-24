/**
 * Workflow 1 — Device HTTP: validation responses before DB is touched.
 * Integration slice: set RUN_INTEGRATION_TESTS=1 and working MYSQL_* to run DB-backed tests.
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import request from 'supertest'
import { createApp } from '../src/app.js'

const app = createApp()
const integration = process.env.RUN_INTEGRATION_TESTS === '1'

test('POST /device/email returns 400 for invalid body', async () => {
  const res = await request(app)
    .post('/device/email')
    .send({ device_id: 'x', email: 'not-email' })
    .expect(400)
  assert.equal(res.body.error, 'validation_error')
})

test('GET /device/email returns 400 without device_id', async () => {
  const res = await request(app).get('/device/email').expect(400)
  assert.equal(res.body.error, 'validation_error')
})

test('GET /device/email returns 404 for unknown device', { skip: !integration }, async () => {
  const res = await request(app)
    .get('/device/email')
    .query({ device_id: 'nonexistent-device-xyz-999' })
    .expect(404)
  assert.equal(res.body.error, 'not_found')
})

test('POST /device/email then GET round-trip', { skip: !integration }, async () => {
  const id = `itest-${Date.now()}`
  const email = `itest+${Date.now()}@example.com`
  await request(app).post('/device/email').send({ device_id: id, email }).expect(200)
  const res = await request(app).get('/device/email').query({ device_id: id }).expect(200)
  assert.equal(res.body.email, email)
  assert.equal(res.body.device_id, id)
})
