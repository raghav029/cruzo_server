-- Tenant timezone
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Kolkata';

-- Skip cutoff on cancellation config
ALTER TABLE cancellation_configs ADD COLUMN IF NOT EXISTS skip_cutoff_hour INT NOT NULL DEFAULT 22;

-- Daily Schedules
CREATE TABLE daily_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    corporate_client_id UUID NOT NULL REFERENCES corporate_clients(id),
    name VARCHAR(255) NOT NULL,
    vehicle_type VARCHAR(20) NOT NULL,
    recurrence_days VARCHAR(50) NOT NULL,
    pickup_time TIME NOT NULL,
    drop_address TEXT NOT NULL,
    drop_lat DECIMAL(9,6),
    drop_lng DECIMAL(9,6),
    is_pooled BOOLEAN NOT NULL DEFAULT false,
    max_capacity INT NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_by_user_id UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Daily Schedule Passengers (enrollment)
CREATE TABLE daily_schedule_passengers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    daily_schedule_id UUID NOT NULL REFERENCES daily_schedules(id),
    employee_user_id UUID NOT NULL REFERENCES users(id),
    pickup_address TEXT NOT NULL,
    pickup_lat DECIMAL(9,6),
    pickup_lng DECIMAL(9,6),
    stop_sequence INT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    enrolled_by_user_id UUID REFERENCES users(id),
    sequence_assigned_by_user_id UUID REFERENCES users(id),
    sequence_assigned_at TIMESTAMPTZ,
    enrolled_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(daily_schedule_id, employee_user_id)
);

-- Daily Trips (created by scheduler)
CREATE TABLE daily_trips (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    daily_schedule_id UUID NOT NULL REFERENCES daily_schedules(id),
    trip_date DATE NOT NULL,
    scheduled_pickup_time TIME NOT NULL,
    drop_address TEXT NOT NULL,
    driver_id UUID REFERENCES drivers(id),
    vehicle_id UUID REFERENCES vehicles(id),
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    driver_alert_sent BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(daily_schedule_id, trip_date)
);

-- Daily Trip Passengers
CREATE TABLE daily_trip_passengers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    daily_trip_id UUID NOT NULL REFERENCES daily_trips(id),
    employee_user_id UUID NOT NULL REFERENCES users(id),
    pickup_address TEXT NOT NULL,
    pickup_lat DECIMAL(9,6),
    pickup_lng DECIMAL(9,6),
    stop_sequence INT,
    boarding_otp VARCHAR(6) NOT NULL,
    drop_otp VARCHAR(6) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    boarding_verified_at TIMESTAMPTZ,
    drop_verified_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    cancel_reason TEXT,
    otp_sms_sent BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Skip Dates
CREATE TABLE daily_trip_skip_dates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    daily_schedule_passenger_id UUID NOT NULL REFERENCES daily_schedule_passengers(id),
    skip_date DATE NOT NULL,
    skipped_by_user_id UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(daily_schedule_passenger_id, skip_date)
);
