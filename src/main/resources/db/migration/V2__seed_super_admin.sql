-- Seed SUPER_ADMIN for local dev / first boot
-- Password: Admin@1234
INSERT INTO users (id, tenant_id, full_name, email, password_hash, role, status, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    NULL,
    'Super Admin',
    'admin@carbooking.com',
    '$2b$12$KlJSpbNQwO0eq/vmumT0HOOvMZZRPMJIHPxwhcgL9AwnHm5tWTWlS',
    'SUPER_ADMIN',
    'ACTIVE',
    NOW(),
    NOW()
);
