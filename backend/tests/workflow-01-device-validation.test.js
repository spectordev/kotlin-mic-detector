/**
 * Workflow 1 — Device registration (validation only, no HTTP / DB).
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import {
  deviceEmailBodySchema,
  deviceEmailQuerySchema,
} from '../src/middleware/validate.js'

test('deviceEmailBodySchema accepts valid payload', () => {
  const v = deviceEmailBodySchema.parse({
    device_id: 'test-device-1',
    email: 'user@example.com',
  })
  assert.equal(v.email, 'user@example.com')
})

test('deviceEmailBodySchema rejects invalid email', () => {
  const r = deviceEmailBodySchema.safeParse({
    device_id: 'x',
    email: 'not-an-email',
  })
  assert.equal(r.success, false)
})

test('deviceEmailBodySchema rejects empty device_id', () => {
  const r = deviceEmailBodySchema.safeParse({
    device_id: '',
    email: 'a@b.com',
  })
  assert.equal(r.success, false)
})

test('deviceEmailQuerySchema accepts device_id query shape', () => {
  const v = deviceEmailQuerySchema.parse({ device_id: 'abc-123' })
  assert.equal(v.device_id, 'abc-123')
})

test('deviceEmailQuerySchema rejects missing device_id', () => {
  const r = deviceEmailQuerySchema.safeParse({})
  assert.equal(r.success, false)
})
