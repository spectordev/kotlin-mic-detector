/**
 * Applies sql/schema.sql to MYSQL_DATABASE. Run: npm run db:init
 */
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'
import mysql from 'mysql2/promise'
import { config } from '../config.js'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const schemaPath = path.join(__dirname, '..', '..', 'sql', 'schema.sql')

async function main() {
  const sql = fs.readFileSync(schemaPath, 'utf8')
  const conn = await mysql.createConnection({
    host: config.mysql.host,
    port: config.mysql.port,
    user: config.mysql.user,
    password: config.mysql.password,
    database: config.mysql.database,
    multipleStatements: true,
  })
  try {
    await conn.query(sql)
    console.log('Schema applied OK:', config.mysql.database)
  } finally {
    await conn.end()
  }
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
