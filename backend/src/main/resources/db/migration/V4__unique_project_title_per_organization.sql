CREATE UNIQUE INDEX uk_projects_organization_title_lower ON projects (organization_id, LOWER(title));
