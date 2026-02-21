-- Execute this script in your PostgreSQL database (tool like pgAdmin, DBeaver, or psql)

-- 1. Add telegram_chat_id to c_users table
ALTER TABLE c_users ADD COLUMN IF NOT EXISTS telegram_chat_id BIGINT;

-- 2. Add category and title to c_notifications table
ALTER TABLE c_notifications ADD COLUMN IF NOT EXISTS category VARCHAR(255);
ALTER TABLE c_notifications ADD COLUMN IF NOT EXISTS title VARCHAR(255);

-- 3. (Optional) Update existing notifications to have a default category
UPDATE c_notifications SET category = 'SYSTEM' WHERE category IS NULL;

-- 4. Add status to c_users and set existing to 'ACTIVE'
ALTER TABLE c_users ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
UPDATE c_users SET status = 'ACTIVE' WHERE status IS NULL;

-- 5. Transition email to phone
DO $$
BEGIN
    -- Case 1: email exists, phone doesn't -> Rename
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'c_users' AND column_name = 'email') 
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'c_users' AND column_name = 'phone') THEN
        ALTER TABLE c_users RENAME COLUMN email TO phone;
        ALTER TABLE c_users ALTER COLUMN phone SET NOT NULL;
    
    -- Case 2: both exist -> Sync data and drop email
    ELSIF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'c_users' AND column_name = 'email') 
          AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'c_users' AND column_name = 'phone') THEN
        UPDATE c_users SET phone = email WHERE phone IS NULL;
        ALTER TABLE c_users DROP COLUMN email;
        ALTER TABLE c_users ALTER COLUMN phone SET NOT NULL;
    END IF;
END $$;

-- 6. Add uniqueness constraint if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'unique_phone') THEN
        ALTER TABLE c_users ADD CONSTRAINT unique_phone UNIQUE (phone);
    END IF;
END $$;
