-- V7: Add occasion to bookings, create addons + booking_addons tables

ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS occasion VARCHAR(50);

CREATE TABLE IF NOT EXISTS addons (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID        NOT NULL REFERENCES tenants(id),
    name       VARCHAR(100) NOT NULL,
    price      NUMERIC(10,2) NOT NULL DEFAULT 0,
    active     BOOLEAN     NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS booking_addons (
    booking_id     UUID          NOT NULL REFERENCES bookings(id),
    addon_id       UUID          NOT NULL REFERENCES addons(id),
    quantity       INTEGER       NOT NULL DEFAULT 1,
    price_snapshot NUMERIC(10,2) NOT NULL,
    PRIMARY KEY (booking_id, addon_id)
);
