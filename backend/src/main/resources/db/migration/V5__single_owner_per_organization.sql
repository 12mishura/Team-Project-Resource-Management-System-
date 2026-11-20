CREATE UNIQUE INDEX uk_organization_members_single_owner ON organization_members (organization_id) WHERE role = 'OWNER';
