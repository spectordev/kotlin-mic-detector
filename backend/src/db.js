import mysql from 'mysql2/promise'
import { config } from './config.js'

let pool

export function getPool() {
  if (!pool) {
    pool = mysql.createPool({
      host: config.mysql.host,
      port: config.mysql.port,
      user: config.mysql.user,
      password: config.mysql.password,
      database: config.mysql.database,
      waitForConnections: true,
      connectionLimit: 10,
      queueLimit: 0,
    })
  }
  return pool
}

export async function query(sql, params = []) {
  const p = getPool()
  const [rows] = await p.execute(sql, params)
  return rows
}
