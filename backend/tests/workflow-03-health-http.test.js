/**
 * Workflow 0 — API liveness (no DB).
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import request from 'supertest'
import { createApp } from '../src/app.js'

test('GET /health returns ok', async () => {
  const app = createApp()
  const res = await request(app).get('/health').expect(200)
  assert.equal(res.body.ok, true)
})
