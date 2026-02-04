-- Execute this script in your PostgreSQL database (tool like pgAdmin, DBeaver, or psql)

-- 1. Add telegram_chat_id to c_users table
ALTER TABLE c_users ADD COLUMN IF NOT EXISTS telegram_chat_id BIGINT;

-- 2. Add category and title to c_notifications table
ALTER TABLE c_notifications ADD COLUMN IF NOT EXISTS category VARCHAR(255);
ALTER TABLE c_notifications ADD COLUMN IF NOT EXISTS title VARCHAR(255);

-- 3. (Optional) Update existing notifications to have a default category
UPDATE c_notifications SET category = 'SYSTEM' WHERE category IS NULL;
