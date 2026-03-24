/**
 * Workflow 2 — Upload step: recipient email validation (same rules as POST /recordings/upload).
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { recipientEmailSchema } from '../src/middleware/validate.js'

test('recipientEmailSchema accepts standard address', () => {
  assert.equal(recipientEmailSchema.parse('  user@example.com  '), 'user@example.com')
})

test('recipientEmailSchema rejects invalid address', () => {
  assert.equal(recipientEmailSchema.safeParse('bad').success, false)
})

test('recipientEmailSchema rejects empty string', () => {
  assert.equal(recipientEmailSchema.safeParse('').success, false)
})
