-- Add methodology column to checklist_items
ALTER TABLE checklist_items ADD COLUMN methodology TEXT;

-- Add methodology column to checklist_template_items
ALTER TABLE checklist_template_items ADD COLUMN methodology TEXT;
