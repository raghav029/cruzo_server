CREATE TABLE promo_codes (
    id                  UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID          NOT NULL REFERENCES tenants(id),
    code                VARCHAR(50)   NOT NULL,
    description         TEXT,
    discount_type       VARCHAR(20)   NOT NULL,          -- FLAT | PERCENTAGE
    discount_value      NUMERIC(10,2) NOT NULL,
    min_booking_value   NUMERIC(10,2),                   -- null = no minimum
    max_discount_amount NUMERIC(10,2),                   -- cap for PERCENTAGE, null = no cap
    max_uses            INTEGER,                         -- null = unlimited
    used_count          INTEGER       NOT NULL DEFAULT 0,
    valid_from          TIMESTAMPTZ   NOT NULL,
    valid_until         TIMESTAMPTZ,                     -- null = no expiry
    active              BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_promo_code_per_tenant UNIQUE (tenant_id, code)
);

CREATE INDEX idx_promo_codes_tenant ON promo_codes(tenant_id);
CREATE INDEX idx_promo_codes_code   ON promo_codes(tenant_id, code);

-- Add promo fields to bookings (B2C bookings only, nullable)
ALTER TABLE bookings
    ADD COLUMN promo_code       VARCHAR(50),
    ADD COLUMN discount_amount  NUMERIC(10,2);
