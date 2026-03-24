import { createApp } from './app.js'
import { config } from './config.js'
import { cleanupOldRecordings } from './services/cleanup.js'
import { retryStaleRecordings } from './services/recordingProcessor.js'
import { logDbMaintenanceError } from './utils/throttledLog.js'

const app = createApp()

const server = app.listen(config.port, () => {
  console.log(`BgApp backend listening on http://0.0.0.0:${config.port}`)
})

async function runRetryStale() {
  try {
    await retryStaleRecordings()
  } catch (e) {
    logDbMaintenanceError('retryStaleRecordings', e)
  }
}

async function runCleanup() {
  try {
    await cleanupOldRecordings()
  } catch (e) {
    logDbMaintenanceError('cleanupOldRecordings', e)
  }
}

setInterval(runRetryStale, 5 * 60 * 1000)

setInterval(runCleanup, config.cleanupIntervalMs)

function shutdown() {
  server.close(() => process.exit(0))
}
process.on('SIGINT', shutdown)
process.on('SIGTERM', shutdown)
