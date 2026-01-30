-- Migration script to fix c_tasks_status_check constraint
-- This removes the REWORK status from the database constraint

-- Step 1: Drop the old constraint
ALTER TABLE c_tasks DROP CONSTRAINT IF EXISTS c_tasks_status_check;

-- Step 2: Create new constraint without REWORK status
ALTER TABLE c_tasks ADD CONSTRAINT c_tasks_status_check 
    CHECK (status IN ('LOCKED', 'ACTIVE', 'UNDER_REVIEW_FOREMAN', 'UNDER_REVIEW_PM', 'REWORK_FOREMAN', 'REWORK_PM', 'COMPLETED'));

-- Step 3: Update any existing tasks with REWORK status to REWORK_FOREMAN
-- (This is a safety measure in case any tasks still have the old status)
UPDATE c_tasks SET status = 'REWORK_FOREMAN' WHERE status = 'REWORK';

-- Verify the changes
SELECT DISTINCT status FROM c_tasks ORDER BY status;
