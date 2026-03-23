-- Phase 1: devices store a default email per logical device (optional; app may still send recipient per upload).

CREATE TABLE IF NOT EXISTS devices (
  id VARCHAR(64) NOT NULL PRIMARY KEY,
  email VARCHAR(320) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Phase 2–4: each uploaded clip + delivery state for retries / cleanup.

CREATE TABLE IF NOT EXISTS recordings (
  id CHAR(36) NOT NULL PRIMARY KEY,
  device_id VARCHAR(64) NULL,
  recipient_email VARCHAR(320) NOT NULL,
  original_filename VARCHAR(255) NOT NULL,
  stored_path VARCHAR(1024) NOT NULL,
  mime_type VARCHAR(128) NOT NULL DEFAULT 'audio/wav',
  file_size BIGINT UNSIGNED NOT NULL,
  status ENUM('pending', 'sending', 'sent', 'failed') NOT NULL DEFAULT 'pending',
  attempts INT UNSIGNED NOT NULL DEFAULT 0,
  last_error TEXT NULL,
  sent_at TIMESTAMP NULL DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_recordings_status (status),
  INDEX idx_recordings_created (created_at),
  INDEX idx_recordings_device (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
