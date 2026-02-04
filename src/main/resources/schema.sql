-- Auto-generated schema migration script to resolve startup crashes
-- This runs BEFORE Hibernate to ensure the DB structure matches the Java Entities

-- 1. Add telegram_chat_id to c_users
ALTER TABLE c_users ADD COLUMN IF NOT EXISTS telegram_chat_id BIGINT;

-- 2. Add category and title to c_notifications
ALTER TABLE c_notifications ADD COLUMN IF NOT EXISTS category VARCHAR(255);
ALTER TABLE c_notifications ADD COLUMN IF NOT EXISTS title VARCHAR(255);

-- 3. Set default category for existing records (avoid null issues)
UPDATE c_notifications SET category = 'SYSTEM' WHERE category IS NULL;
