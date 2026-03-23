import test from 'node:test'
import assert from 'node:assert/strict'
import { deviceEmailBodySchema } from '../src/middleware/validate.js'

test('deviceEmailBodySchema accepts valid payload', () => {
  const v = deviceEmailBodySchema.parse({
    device_id: 'test-device-1',
    email: 'user@example.com',
  })
  assert.equal(v.email, 'user@example.com')
})

test('deviceEmailBodySchema rejects bad email', () => {
  const r = deviceEmailBodySchema.safeParse({
    device_id: 'x',
    email: 'not-an-email',
  })
  assert.equal(r.success, false)
})
