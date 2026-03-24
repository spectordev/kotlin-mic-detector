import { z } from 'zod'

const emailSchema = z.string().trim().email().max(320)

/** Used by POST /recordings/upload (recipient field). */
export const recipientEmailSchema = emailSchema

/** Used by GET /device/email?device_id= */
export const deviceEmailQuerySchema = z.object({
  device_id: z.string().trim().min(1).max(64),
})

export const deviceEmailBodySchema = z.object({
  device_id: z.string().trim().min(1).max(64),
  email: emailSchema,
})

export function validateBody(schema) {
  return (req, res, next) => {
    const parsed = schema.safeParse(req.body)
    if (!parsed.success) {
      return res.status(400).json({
        error: 'validation_error',
        details: parsed.error.flatten(),
      })
    }
    req.validated = parsed.data
    next()
  }
}
