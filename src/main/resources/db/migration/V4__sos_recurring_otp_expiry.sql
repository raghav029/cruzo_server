-- V4: SOS Alerts, Recurring Bookings, Drop OTP, Document Expiry

-- ── Feature 1: SOS Alerts ──────────────────────────────────────────────────
CREATE TABLE sos_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    booking_id UUID REFERENCES bookings(id),
    triggered_by_user_id UUID NOT NULL REFERENCES users(id),
    lat DECIMAL(9,6),
    lng DECIMAL(9,6),
    message TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    resolved_by_user_id UUID REFERENCES users(id),
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ── Feature 2: Recurring Bookings ─────────────────────────────────────────
CREATE TABLE recurring_bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    corporate_client_id UUID NOT NULL REFERENCES corporate_clients(id),
    employee_user_id UUID NOT NULL REFERENCES users(id),
    pickup_address TEXT NOT NULL,
    drop_address TEXT NOT NULL,
    vehicle_type VARCHAR(20) NOT NULL,
    scheduled_time TIME NOT NULL,
    recurrence_days VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ── Feature 3: Drop OTP ───────────────────────────────────────────────────
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS drop_otp VARCHAR(6);
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS otp_generated_at TIMESTAMPTZ;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS otp_verified_at TIMESTAMPTZ;

-- ── Feature 4: Document Expiry ────────────────────────────────────────────
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS insurance_expiry DATE;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS fitness_expiry DATE;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS last_expiry_alert_sent_at TIMESTAMPTZ;

ALTER TABLE drivers ADD COLUMN IF NOT EXISTS insurance_expiry DATE;
ALTER TABLE drivers ADD COLUMN IF NOT EXISTS last_expiry_alert_sent_at TIMESTAMPTZ;
