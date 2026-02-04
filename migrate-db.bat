@echo off
echo Connecting to PostgreSQL and running migration...
echo.

docker exec -i bauberg-postgres-1 psql -U postgres -d postgres << EOF
ALTER TABLE c_users ADD COLUMN IF NOT EXISTS telegram_chat_id BIGINT;
ALTER TABLE c_notifications ADD COLUMN IF NOT EXISTS category VARCHAR(255);
ALTER TABLE c_notifications ADD COLUMN IF NOT EXISTS title VARCHAR(255);
UPDATE c_notifications SET category = 'SYSTEM' WHERE category IS NULL;
EOF

echo.
echo Migration completed!
echo You can now start your Spring Boot application.
pause
