-- Run once as MySQL admin (root). From backend folder, example:
--   mysql -u root -p < sql/bootstrap-mysql.sql
--
-- Default password matches .env.example: bgapp_secret
-- If you change MYSQL_PASSWORD in .env, change IDENTIFIED BY below to match.

CREATE DATABASE IF NOT EXISTS bgapp
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Node mysql2 often uses host 127.0.0.1 (TCP); grant both localhost and 127.0.0.1
CREATE USER IF NOT EXISTS 'bgapp'@'localhost' IDENTIFIED BY 'bgapp_secret';
CREATE USER IF NOT EXISTS 'bgapp'@'127.0.0.1' IDENTIFIED BY 'bgapp_secret';

GRANT ALL PRIVILEGES ON bgapp.* TO 'bgapp'@'localhost';
GRANT ALL PRIVILEGES ON bgapp.* TO 'bgapp'@'127.0.0.1';

FLUSH PRIVILEGES;
