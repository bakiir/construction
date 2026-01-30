-- Migration: Add Employee Assignment System
-- Adds hierarchical assignment: PM -> Project, Foreman -> Project/Object, Worker -> SubObject

-- 1. Add project_manager_id to c_projects
ALTER TABLE c_projects 
ADD COLUMN project_manager_id BIGINT;

ALTER TABLE c_projects
ADD CONSTRAINT fk_project_manager 
    FOREIGN KEY (project_manager_id) 
    REFERENCES c_users(id) ON DELETE SET NULL;

-- 2. Create project_foremen junction table (many-to-many)
CREATE TABLE project_foremen (
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (project_id, user_id),
    CONSTRAINT fk_project_foremen_project 
        FOREIGN KEY (project_id) REFERENCES c_projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_foremen_user 
        FOREIGN KEY (user_id) REFERENCES c_users(id) ON DELETE CASCADE
);

-- 3. Add lead_foreman_id to c_objects (optional)
ALTER TABLE c_objects 
ADD COLUMN lead_foreman_id BIGINT;

ALTER TABLE c_objects
ADD CONSTRAINT fk_lead_foreman 
    FOREIGN KEY (lead_foreman_id) 
    REFERENCES c_users(id) ON DELETE SET NULL;

-- 4. Create sub_object_workers junction table (many-to-many)
CREATE TABLE sub_object_workers (
    sub_object_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (sub_object_id, user_id),
    CONSTRAINT fk_sub_object_workers_sub_object 
        FOREIGN KEY (sub_object_id) REFERENCES c_sub_objects(id) ON DELETE CASCADE,
    CONSTRAINT fk_sub_object_workers_user 
        FOREIGN KEY (user_id) REFERENCES c_users(id) ON DELETE CASCADE
);

-- 5. Create indexes for performance
CREATE INDEX idx_project_manager ON c_projects(project_manager_id);
CREATE INDEX idx_lead_foreman ON c_objects(lead_foreman_id);
CREATE INDEX idx_project_foremen_user ON project_foremen(user_id);
CREATE INDEX idx_project_foremen_project ON project_foremen(project_id);
CREATE INDEX idx_sub_object_workers_user ON sub_object_workers(user_id);
CREATE INDEX idx_sub_object_workers_sub_object ON sub_object_workers(sub_object_id);

-- 6. Optional: Migrate existing task assignees to sub_object_workers
-- This automatically assigns workers to sub-objects based on their current task assignments
INSERT INTO sub_object_workers (sub_object_id, user_id)
SELECT DISTINCT t.sub_obj_id, ta.user_id
FROM c_tasks t
JOIN task_assignees ta ON t.id = ta.task_id
JOIN c_users u ON ta.user_id = u.id
WHERE u.role = 'WORKER'
ON CONFLICT DO NOTHING;

-- Verification queries
-- SELECT * FROM project_foremen;
-- SELECT * FROM sub_object_workers;
-- SELECT p.name, u.full_name as project_manager FROM c_projects p LEFT JOIN c_users u ON p.project_manager_id = u.id;
