-- Clean up any previous run so this script is safe to re-run.
DO $$
DECLARE
    target_tenant_ids uuid[];
BEGIN
    SELECT COALESCE(array_agg(id), ARRAY[]::uuid[])
    INTO target_tenant_ids
    FROM tenants
    WHERE id = '11111111-1111-1111-1111-111111111111'
       OR subdomain = 'acme-fleet';

    DELETE FROM booking_status_history
    WHERE tenant_id = ANY(target_tenant_ids);

    DELETE FROM bookings
    WHERE tenant_id = ANY(target_tenant_ids);

    DELETE FROM corporate_employees
    WHERE tenant_id = ANY(target_tenant_ids);

    DELETE FROM drivers
    WHERE tenant_id = ANY(target_tenant_ids);

    DELETE FROM vehicles
    WHERE tenant_id = ANY(target_tenant_ids);

    DELETE FROM corporate_clients
    WHERE tenant_id = ANY(target_tenant_ids);

    DELETE FROM users
    WHERE tenant_id = ANY(target_tenant_ids);

    DELETE FROM tenants
    WHERE id = ANY(target_tenant_ids);
END $$;

-- Demo IDs (fixed so every insert/update can reference them easily)
-- tenant:            11111111-1111-1111-1111-111111111111
-- fleet manager:     22222222-2222-2222-2222-222222222222
-- corporate admin:   33333333-3333-3333-3333-333333333333
-- employee:          44444444-4444-4444-4444-444444444444
-- driver:            55555555-5555-5555-5555-555555555555
-- corporate client:   66666666-6666-6666-6666-666666666666
-- driver profile:     77777777-7777-7777-7777-777777777777
-- vehicle:            88888888-8888-8888-8888-888888888888
-- booking:            99999999-9999-9999-9999-999999999999

INSERT INTO tenants (
    id, name, subdomain, support_email, support_phone, is_active, created_at, updated_at
) VALUES (
    '11111111-1111-1111-1111-111111111111',
    'Acme Fleet',
    'acme-fleet',
    'support@acmefleet.com',
    '9999999999',
    TRUE,
    NOW(),
    NOW()
);

INSERT INTO users (
    id, tenant_id, email, phone, full_name, password_hash, role, status, created_at, updated_at
) VALUES
(
    '22222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    'fleet.manager@acme.com',
    '9000000001',
    'Fleet Manager',
    '$2b$12$KlJSpbNQwO0eq/vmumT0HOOvMZZRPMJIHPxwhcgL9AwnHm5tWTWlS',
    'FLEET_MANAGER',
    'ACTIVE',
    NOW(),
    NOW()
),
(
    '33333333-3333-3333-3333-333333333333',
    '11111111-1111-1111-1111-111111111111',
    'corp.admin@acme.com',
    '9000000002',
    'Corporate Admin',
    '$2b$12$KlJSpbNQwO0eq/vmumT0HOOvMZZRPMJIHPxwhcgL9AwnHm5tWTWlS',
    'CORPORATE_ADMIN',
    'ACTIVE',
    NOW(),
    NOW()
),
(
    '44444444-4444-4444-4444-444444444444',
    '11111111-1111-1111-1111-111111111111',
    'employee@acme.com',
    '9000000003',
    'Ravi Employee',
    '$2b$12$KlJSpbNQwO0eq/vmumT0HOOvMZZRPMJIHPxwhcgL9AwnHm5tWTWlS',
    'EMPLOYEE',
    'ACTIVE',
    NOW(),
    NOW()
),
(
    '55555555-5555-5555-5555-555555555555',
    '11111111-1111-1111-1111-111111111111',
    'driver@acme.com',
    '9000000004',
    'Arun Driver',
    '$2b$12$KlJSpbNQwO0eq/vmumT0HOOvMZZRPMJIHPxwhcgL9AwnHm5tWTWlS',
    'DRIVER',
    'ACTIVE',
    NOW(),
    NOW()
);

INSERT INTO corporate_clients (
    id, tenant_id, company_name, billing_email, billing_cycle, credit_limit, current_outstanding, is_active, created_at, updated_at
) VALUES (
    '66666666-6666-6666-6666-666666666666',
    '11111111-1111-1111-1111-111111111111',
    'Acme Technologies Pvt Ltd',
    'billing@acme.com',
    'MONTHLY',
    50000.00,
    0.00,
    TRUE,
    NOW(),
    NOW()
);

INSERT INTO vehicles (
    id, tenant_id, plate_number, vehicle_type, make, model, year, color, status, created_at, updated_at
) VALUES (
    '88888888-8888-8888-8888-888888888888',
    '11111111-1111-1111-1111-111111111111',
    'KA01AB1234',
    'SEDAN',
    'Toyota',
    'Etios',
    2022,
    'White',
    'ACTIVE',
    NOW(),
    NOW()
);

INSERT INTO drivers (
    id, tenant_id, user_id, license_number, license_expiry, availability, current_vehicle_id, created_at, updated_at
) VALUES (
    '77777777-7777-7777-7777-777777777777',
    '11111111-1111-1111-1111-111111111111',
    '55555555-5555-5555-5555-555555555555',
    'DL-1234567890',
    DATE '2027-12-31',
    'OFF_DUTY',
    '88888888-8888-8888-8888-888888888888',
    NOW(),
    NOW()
);

INSERT INTO corporate_employees (
    id, tenant_id, corporate_client_id, user_id, employee_code, department, designation, monthly_ride_limit, is_active, created_at, updated_at
) VALUES (
    gen_random_uuid(),
    '11111111-1111-1111-1111-111111111111',
    '66666666-6666-6666-6666-666666666666',
    '44444444-4444-4444-4444-444444444444',
    'EMP-001',
    'Operations',
    'Associate',
    20,
    TRUE,
    NOW(),
    NOW()
);

INSERT INTO bookings (
    id,
    tenant_id,
    corporate_client_id,
    employee_user_id,
    pickup_address,
    drop_address,
    pickup_lat,
    pickup_lng,
    drop_lat,
    drop_lng,
    vehicle_type_requested,
    scheduled_at,
    notes,
    status,
    estimated_fare,
    created_at,
    updated_at
) VALUES (
    '99999999-9999-9999-9999-999999999999',
    '11111111-1111-1111-1111-111111111111',
    '66666666-6666-6666-6666-666666666666',
    '44444444-4444-4444-4444-444444444444',
    'MG Road, Bengaluru',
    'Electronic City, Bengaluru',
    12.975000,
    77.605000,
    12.845000,
    77.660000,
    'SEDAN',
    NOW() + INTERVAL '3 hours',
    'Demo booking for dashboard flow',
    'PENDING_APPROVAL',
    450.00,
    NOW(),
    NOW()
);

INSERT INTO booking_status_history (
    tenant_id, booking_id, from_status, to_status, actor_user_id, reason, transitioned_at, created_at
) VALUES (
    '11111111-1111-1111-1111-111111111111',
    '99999999-9999-9999-9999-999999999999',
    NULL,
    'PENDING_APPROVAL',
    '44444444-4444-4444-4444-444444444444',
    'Booking created',
    NOW(),
    NOW()
);

-- At this point the fleet manager dashboard should show:
-- pendingApprovals = 1
-- activeTrips = 0
SELECT
    status,
    COUNT(*) AS booking_count
FROM bookings
WHERE tenant_id = '11111111-1111-1111-1111-111111111111'
GROUP BY status
ORDER BY status;

-- Step 1: approve it
UPDATE bookings
SET status = 'APPROVED',
    approved_at = NOW(),
    updated_at = NOW()
WHERE id = '99999999-9999-9999-9999-999999999999';

INSERT INTO booking_status_history (
    tenant_id, booking_id, from_status, to_status, actor_user_id, reason, transitioned_at, created_at
) VALUES (
    '11111111-1111-1111-1111-111111111111',
    '99999999-9999-9999-9999-999999999999',
    'PENDING_APPROVAL',
    'APPROVED',
    '33333333-3333-3333-3333-333333333333',
    NULL,
    NOW(),
    NOW()
);

-- Step 2: assign driver + vehicle
UPDATE bookings
SET status = 'DRIVER_ASSIGNED',
    driver_id = '77777777-7777-7777-7777-777777777777',
    vehicle_id = '88888888-8888-8888-8888-888888888888',
    assigned_by_user_id = '22222222-2222-2222-2222-222222222222',
    assignment_mode = 'MANUAL',
    driver_assigned_at = NOW(),
    updated_at = NOW()
WHERE id = '99999999-9999-9999-9999-999999999999';

UPDATE drivers
SET availability = 'ON_TRIP',
    current_vehicle_id = '88888888-8888-8888-8888-888888888888',
    updated_at = NOW()
WHERE id = '77777777-7777-7777-7777-777777777777';

UPDATE vehicles
SET status = 'IN_TRIP',
    updated_at = NOW()
WHERE id = '88888888-8888-8888-8888-888888888888';

INSERT INTO booking_status_history (
    tenant_id, booking_id, from_status, to_status, actor_user_id, reason, transitioned_at, created_at
) VALUES (
    '11111111-1111-1111-1111-111111111111',
    '99999999-9999-9999-9999-999999999999',
    'APPROVED',
    'DRIVER_ASSIGNED',
    '22222222-2222-2222-2222-222222222222',
    NULL,
    NOW(),
    NOW()
);

-- Step 3: move into active trip state
UPDATE bookings
SET status = 'DRIVER_EN_ROUTE',
    updated_at = NOW()
WHERE id = '99999999-9999-9999-9999-999999999999';

INSERT INTO booking_status_history (
    tenant_id, booking_id, from_status, to_status, actor_user_id, reason, transitioned_at, created_at
) VALUES (
    '11111111-1111-1111-1111-111111111111',
    '99999999-9999-9999-9999-999999999999',
    'DRIVER_ASSIGNED',
    'DRIVER_EN_ROUTE',
    '55555555-5555-5555-5555-555555555555',
    NULL,
    NOW(),
    NOW()
),
(
    '11111111-1111-1111-1111-111111111111',
    '99999999-9999-9999-9999-999999999999',
    'DRIVER_EN_ROUTE',
    'ARRIVED',
    '55555555-5555-5555-5555-555555555555',
    NULL,
    NOW(),
    NOW()
);

UPDATE bookings
SET status = 'IN_PROGRESS',
    trip_started_at = NOW(),
    updated_at = NOW()
WHERE id = '99999999-9999-9999-9999-999999999999';

INSERT INTO booking_status_history (
    tenant_id, booking_id, from_status, to_status, actor_user_id, reason, transitioned_at, created_at
) VALUES (
    '11111111-1111-1111-1111-111111111111',
    '99999999-9999-9999-9999-999999999999',
    'ARRIVED',
    'IN_PROGRESS',
    '55555555-5555-5555-5555-555555555555',
    'OTP verified',
    NOW(),
    NOW()
);

-- This is the state the dashboard treats as an active trip:
-- pendingApprovals = 0
-- activeTrips = 1
SELECT
    status,
    COUNT(*) AS booking_count
FROM bookings
WHERE tenant_id = '11111111-1111-1111-1111-111111111111'
GROUP BY status
ORDER BY status;

SELECT
    id,
    status,
    approved_at,
    driver_assigned_at,
    trip_started_at,
    driver_id,
    vehicle_id
FROM bookings
WHERE id = '99999999-9999-9999-9999-999999999999';
