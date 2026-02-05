ALTER TABLE task ADD COLUMN template_id BIGINT;

ALTER TABLE task
ADD CONSTRAINT fk_task_template
FOREIGN KEY (template_id) REFERENCES checklist_template(id);
