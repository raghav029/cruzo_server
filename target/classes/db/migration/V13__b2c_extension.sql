-- 1. Tenant booking mode
ALTER TABLE tenants
    ADD COLUMN booking_mode VARCHAR(20) NOT NULL DEFAULT 'CORPORATE';

-- 2. Vehicle B2C fields
ALTER TABLE vehicles
    ADD COLUMN category     VARCHAR(30),
    ADD COLUMN is_published BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN bag_capacity SMALLINT,
    ADD COLUMN description  TEXT;

-- 3. Vehicle amenities (element collection table)
CREATE TABLE vehicle_amenities (
    vehicle_id UUID        NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    amenity    VARCHAR(50) NOT NULL,
    PRIMARY KEY (vehicle_id, amenity)
);

-- 4. Vehicle pricing packages
CREATE TABLE vehicle_packages (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id       UUID         NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    name             VARCHAR(100) NOT NULL,
    base_rental      NUMERIC(10,2) NOT NULL,
    included_km      INTEGER      NOT NULL DEFAULT 0,
    included_hours   INTEGER      NOT NULL DEFAULT 0,
    extra_per_km     NUMERIC(10,2) NOT NULL DEFAULT 0,
    extra_per_hour   NUMERIC(10,2) NOT NULL DEFAULT 0,
    drive_batta      NUMERIC(10,2) NOT NULL DEFAULT 0,
    outstation_batta NUMERIC(10,2) NOT NULL DEFAULT 0,
    night_batta      NUMERIC(10,2) NOT NULL DEFAULT 0,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 5. Vehicle images
CREATE TABLE vehicle_images (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id    UUID        NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    image_url     TEXT        NOT NULL,
    display_order SMALLINT    NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 6. B2C customers
CREATE TABLE customers (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID         NOT NULL REFERENCES tenants(id),
    name           VARCHAR(100) NOT NULL,
    phone          VARCHAR(20)  NOT NULL,
    email          VARCHAR(150),
    otp_code       VARCHAR(6),
    otp_expires_at TIMESTAMPTZ,
    is_verified    BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, phone)
);

-- 7. B2C columns on bookings
ALTER TABLE bookings
    ALTER COLUMN employee_user_id    DROP NOT NULL,
    ALTER COLUMN corporate_client_id DROP NOT NULL,
    ADD COLUMN booking_type              VARCHAR(20)   NOT NULL DEFAULT 'CORPORATE',
    ADD COLUMN customer_id               UUID REFERENCES customers(id),
    ADD COLUMN package_id                UUID REFERENCES vehicle_packages(id),
    ADD COLUMN extra_km                  NUMERIC(8,2),
    ADD COLUMN extra_hours               NUMERIC(8,2),
    ADD COLUMN drive_batta_applied       NUMERIC(10,2),
    ADD COLUMN outstation_batta_applied  NUMERIC(10,2),
    ADD COLUMN night_batta_applied       NUMERIC(10,2),
    ADD COLUMN parking_fee               NUMERIC(10,2),
    ADD COLUMN toll_fee                  NUMERIC(10,2),
    ADD COLUMN gst_amount                NUMERIC(10,2);

-- 8. Contact enquiries
CREATE TABLE contact_enquiries (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID         NOT NULL REFERENCES tenants(id),
    name       VARCHAR(100) NOT NULL,
    email      VARCHAR(150),
    phone      VARCHAR(20),
    message    TEXT         NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 9. Indexes
CREATE INDEX idx_customers_tenant_phone    ON customers(tenant_id, phone);
CREATE INDEX idx_vehicle_packages_vehicle  ON vehicle_packages(vehicle_id);
CREATE INDEX idx_vehicle_images_vehicle    ON vehicle_images(vehicle_id, display_order);
CREATE INDEX idx_bookings_customer         ON bookings(customer_id);
CREATE INDEX idx_bookings_booking_type     ON bookings(booking_type);
CREATE INDEX idx_contact_enquiries_tenant  ON contact_enquiries(tenant_id);
CREATE INDEX idx_vehicles_tenant_published ON vehicles(tenant_id, is_published);
