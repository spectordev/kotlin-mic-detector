import { createApp } from './app.js'
import { config } from './config.js'
import { cleanupOldRecordings } from './services/cleanup.js'
import { retryStaleRecordings } from './services/recordingProcessor.js'

const app = createApp()

const server = app.listen(config.port, () => {
  console.log(`BgApp backend listening on http://0.0.0.0:${config.port}`)
})

setInterval(() => {
  retryStaleRecordings().catch((e) => console.error('retryStaleRecordings', e))
}, 5 * 60 * 1000)

setInterval(() => {
  cleanupOldRecordings().catch((e) => console.error('cleanupOldRecordings', e))
}, config.cleanupIntervalMs)

function shutdown() {
  server.close(() => process.exit(0))
}
process.on('SIGINT', shutdown)
process.on('SIGTERM', shutdown)
