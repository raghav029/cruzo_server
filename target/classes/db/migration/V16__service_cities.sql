CREATE TABLE service_cities (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID         NOT NULL REFERENCES tenants(id),
    name       VARCHAR(100) NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_city_per_tenant UNIQUE (tenant_id, name)
);

CREATE INDEX idx_service_cities_tenant ON service_cities(tenant_id);

ALTER TABLE vehicles ADD COLUMN city_id UUID REFERENCES service_cities(id);

ALTER TABLE bookings ADD COLUMN city_id UUID REFERENCES service_cities(id);
