-- ============================================================
-- WHITE-LABEL CORPORATE CAR BOOKING SAAS — POSTGRESQL SCHEMA
-- City: Bangalore | V1 | Postpaid Corporate Invoicing
-- ============================================================


-- ============================================================
-- UPDATED_AT TRIGGER FUNCTION (shared across all tables)
-- ============================================================

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- ============================================================
-- ENUMS
-- ============================================================

-- Enums stored as TEXT for Hibernate compatibility


-- ============================================================
-- TABLE: tenants
-- Platform tenants — each is a fleet manager who bought the SaaS
-- ============================================================

CREATE TABLE tenants (
  id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  name              TEXT          NOT NULL,
  subdomain         TEXT          NOT NULL UNIQUE,
  logo_url          TEXT,
  primary_color     VARCHAR(7),
  secondary_color   VARCHAR(7),
  support_email     TEXT,
  support_phone     VARCHAR(20),
  is_active         BOOLEAN       NOT NULL DEFAULT TRUE,
  created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
  updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_tenants_subdomain ON tenants (subdomain);
CREATE INDEX idx_tenants_is_active ON tenants (is_active);

CREATE TRIGGER trg_tenants_updated_at
  BEFORE UPDATE ON tenants
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ============================================================
-- TABLE: users
-- All actors in one table; SUPER_ADMIN has NULL tenant_id
-- ============================================================

CREATE TABLE users (
  id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id       UUID          REFERENCES tenants(id) ON DELETE RESTRICT,
  email           TEXT          NOT NULL,
  phone           VARCHAR(20),
  full_name       TEXT          NOT NULL,
  password_hash   TEXT          NOT NULL,
  role            TEXT          NOT NULL,
  status          TEXT          NOT NULL DEFAULT 'ACTIVE',
  last_login_at   TIMESTAMPTZ,
  created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

  CONSTRAINT chk_super_admin_no_tenant
    CHECK (
      (role = 'SUPER_ADMIN' AND tenant_id IS NULL) OR
      (role <> 'SUPER_ADMIN' AND tenant_id IS NOT NULL)
    ),

  CONSTRAINT uq_users_email_tenant UNIQUE (tenant_id, email)
);

CREATE INDEX idx_users_tenant_id    ON users (tenant_id);
CREATE INDEX idx_users_role         ON users (role);
CREATE INDEX idx_users_status       ON users (status);
CREATE INDEX idx_users_email        ON users (email);

CREATE TRIGGER trg_users_updated_at
  BEFORE UPDATE ON users
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ============================================================
-- TABLE: vehicles
-- Fleet vehicles belonging to a tenant
-- ============================================================

CREATE TABLE vehicles (
  id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id       UUID            NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
  plate_number    TEXT            NOT NULL,
  vehicle_type    TEXT            NOT NULL,
  make            TEXT,
  model           TEXT,
  year            SMALLINT,
  color           TEXT,
  status          TEXT            NOT NULL DEFAULT 'ACTIVE',
  created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

  CONSTRAINT uq_vehicles_plate_tenant UNIQUE (tenant_id, plate_number)
);

CREATE INDEX idx_vehicles_tenant_id     ON vehicles (tenant_id);
CREATE INDEX idx_vehicles_status        ON vehicles (status);
CREATE INDEX idx_vehicles_vehicle_type  ON vehicles (vehicle_type);

CREATE TRIGGER trg_vehicles_updated_at
  BEFORE UPDATE ON vehicles
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ============================================================
-- TABLE: drivers
-- Driver profile linked to a user account (role = DRIVER)
-- ============================================================

CREATE TABLE drivers (
  id                  UUID                  PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id           UUID                  NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
  user_id             UUID                  NOT NULL UNIQUE REFERENCES users(id) ON DELETE RESTRICT,
  license_number      TEXT                  NOT NULL,
  license_expiry      DATE                  NOT NULL,
  availability        TEXT                  NOT NULL DEFAULT 'OFF_DUTY',
  current_vehicle_id  UUID                  REFERENCES vehicles(id) ON DELETE SET NULL,
  created_at          TIMESTAMPTZ           NOT NULL DEFAULT NOW(),
  updated_at          TIMESTAMPTZ           NOT NULL DEFAULT NOW(),

  CONSTRAINT uq_drivers_license_tenant UNIQUE (tenant_id, license_number)
);

CREATE INDEX idx_drivers_tenant_id    ON drivers (tenant_id);
CREATE INDEX idx_drivers_user_id      ON drivers (user_id);
CREATE INDEX idx_drivers_availability ON drivers (availability);

CREATE TRIGGER trg_drivers_updated_at
  BEFORE UPDATE ON drivers
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ============================================================
-- TABLE: corporate_clients
-- Companies contracted by a tenant (fleet manager)
-- ============================================================

CREATE TABLE corporate_clients (
  id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id           UUID            NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
  company_name        TEXT            NOT NULL,
  gst_number          TEXT,
  billing_address     TEXT,
  billing_email       TEXT            NOT NULL,
  billing_cycle       TEXT            NOT NULL DEFAULT 'MONTHLY',
  credit_limit        NUMERIC(12, 2)  NOT NULL DEFAULT 0,
  current_outstanding NUMERIC(12, 2)  NOT NULL DEFAULT 0,
  is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
  created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
  updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

  CONSTRAINT uq_corporate_clients_tenant_name UNIQUE (tenant_id, company_name)
);

CREATE INDEX idx_corporate_clients_tenant_id ON corporate_clients (tenant_id);
CREATE INDEX idx_corporate_clients_is_active ON corporate_clients (is_active);

CREATE TRIGGER trg_corporate_clients_updated_at
  BEFORE UPDATE ON corporate_clients
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ============================================================
-- TABLE: corporate_employees
-- Employees of a corporate client, linked to a user account (role = EMPLOYEE)
-- ============================================================

CREATE TABLE corporate_employees (
  id                    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id             UUID        NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
  corporate_client_id   UUID        NOT NULL REFERENCES corporate_clients(id) ON DELETE RESTRICT,
  user_id               UUID        NOT NULL UNIQUE REFERENCES users(id) ON DELETE RESTRICT,
  employee_code         TEXT,
  department            TEXT,
  designation           TEXT,
  monthly_ride_limit    INTEGER,
  is_active             BOOLEAN     NOT NULL DEFAULT TRUE,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_corporate_employees_tenant_id           ON corporate_employees (tenant_id);
CREATE INDEX idx_corporate_employees_corporate_client_id ON corporate_employees (corporate_client_id);
CREATE INDEX idx_corporate_employees_user_id             ON corporate_employees (user_id);

CREATE TRIGGER trg_corporate_employees_updated_at
  BEFORE UPDATE ON corporate_employees
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ============================================================
-- TABLE: pricing_configs
-- Per-tenant fare configuration with vehicle type multipliers
-- ============================================================

CREATE TABLE pricing_configs (
  id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id               UUID            NOT NULL UNIQUE REFERENCES tenants(id) ON DELETE RESTRICT,
  base_fare               NUMERIC(8, 2)   NOT NULL DEFAULT 0,
  per_km_rate             NUMERIC(8, 2)   NOT NULL DEFAULT 0,
  per_hour_rate           NUMERIC(8, 2)   NOT NULL DEFAULT 0,
  minimum_fare            NUMERIC(8, 2)   NOT NULL DEFAULT 0,
  sedan_multiplier        NUMERIC(4, 2)   NOT NULL DEFAULT 1.00,
  suv_multiplier          NUMERIC(4, 2)   NOT NULL DEFAULT 1.25,
  luxury_multiplier       NUMERIC(4, 2)   NOT NULL DEFAULT 1.75,
  cgst_pct                NUMERIC(5, 2)   NOT NULL DEFAULT 9.00,
  sgst_pct                NUMERIC(5, 2)   NOT NULL DEFAULT 9.00,
  is_active               BOOLEAN         NOT NULL DEFAULT TRUE,
  effective_from          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
  created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
  updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pricing_configs_tenant_id ON pricing_configs (tenant_id);

CREATE TRIGGER trg_pricing_configs_updated_at
  BEFORE UPDATE ON pricing_configs
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ============================================================
-- TABLE: cancellation_configs
-- Per-tenant cancellation policy
-- ============================================================

CREATE TABLE cancellation_configs (
  id                        UUID                    PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id                 UUID                    NOT NULL UNIQUE REFERENCES tenants(id) ON DELETE RESTRICT,
  cancellation_window_hours NUMERIC(4, 1)           NOT NULL DEFAULT 2,
  fee_type                  TEXT                    NOT NULL DEFAULT 'FLAT',
  fee_value                 NUMERIC(8, 2)           NOT NULL DEFAULT 0,
  after_window_allowed      BOOLEAN                 NOT NULL DEFAULT FALSE,
  created_at                TIMESTAMPTZ             NOT NULL DEFAULT NOW(),
  updated_at                TIMESTAMPTZ             NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cancellation_configs_tenant_id ON cancellation_configs (tenant_id);

CREATE TRIGGER trg_cancellation_configs_updated_at
  BEFORE UPDATE ON cancellation_configs
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ============================================================
-- TABLE: bookings
-- Core booking table with full state machine
-- ============================================================

CREATE TABLE bookings (
  id                      UUID              PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id               UUID              NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
  corporate_client_id     UUID              NOT NULL REFERENCES corporate_clients(id) ON DELETE RESTRICT,
  employee_user_id        UUID              NOT NULL REFERENCES users(id) ON DELETE RESTRICT,

  driver_id               UUID              REFERENCES drivers(id) ON DELETE SET NULL,
  vehicle_id              UUID              REFERENCES vehicles(id) ON DELETE SET NULL,
  assigned_by_user_id     UUID              REFERENCES users(id) ON DELETE SET NULL,
  assignment_mode         TEXT,

  pickup_address          TEXT              NOT NULL,
  drop_address            TEXT              NOT NULL,
  pickup_lat              NUMERIC(9, 6),
  pickup_lng              NUMERIC(9, 6),
  drop_lat                NUMERIC(9, 6),
  drop_lng                NUMERIC(9, 6),
  vehicle_type_requested  TEXT              NOT NULL,
  scheduled_at            TIMESTAMPTZ       NOT NULL,
  notes                   TEXT,

  status                  TEXT              NOT NULL DEFAULT 'DRAFT',
  cancellation_reason     TEXT,
  rejection_reason        TEXT,

  estimated_fare          NUMERIC(10, 2),
  final_fare              NUMERIC(10, 2),
  cancellation_fee        NUMERIC(10, 2),

  approved_at             TIMESTAMPTZ,
  driver_assigned_at      TIMESTAMPTZ,
  trip_started_at         TIMESTAMPTZ,
  trip_completed_at       TIMESTAMPTZ,
  cancelled_at            TIMESTAMPTZ,

  created_at              TIMESTAMPTZ       NOT NULL DEFAULT NOW(),
  updated_at              TIMESTAMPTZ       NOT NULL DEFAULT NOW(),

  CONSTRAINT chk_booking_scheduled_at
    CHECK (scheduled_at >= created_at + INTERVAL '2 hours')
);

CREATE INDEX idx_bookings_tenant_id           ON bookings (tenant_id);
CREATE INDEX idx_bookings_corporate_client_id ON bookings (corporate_client_id);
CREATE INDEX idx_bookings_employee_user_id    ON bookings (employee_user_id);
CREATE INDEX idx_bookings_driver_id           ON bookings (driver_id);
CREATE INDEX idx_bookings_vehicle_id          ON bookings (vehicle_id);
CREATE INDEX idx_bookings_status              ON bookings (status);
CREATE INDEX idx_bookings_scheduled_at        ON bookings (scheduled_at);
CREATE INDEX idx_bookings_tenant_status       ON bookings (tenant_id, status);
CREATE INDEX idx_bookings_tenant_scheduled    ON bookings (tenant_id, scheduled_at DESC);
CREATE INDEX idx_bookings_client_status       ON bookings (corporate_client_id, status);

CREATE TRIGGER trg_bookings_updated_at
  BEFORE UPDATE ON bookings
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ============================================================
-- TABLE: booking_status_history
-- Immutable audit trail of every booking state transition
-- ============================================================

CREATE TABLE booking_status_history (
  id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id       UUID            NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
  booking_id      UUID            NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
  from_status     TEXT,
  to_status       TEXT            NOT NULL,
  actor_user_id   UUID            REFERENCES users(id) ON DELETE SET NULL,
  reason          TEXT,
  transitioned_at TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
  created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_bsh_booking_id   ON booking_status_history (booking_id);
CREATE INDEX idx_bsh_tenant_id    ON booking_status_history (tenant_id);
CREATE INDEX idx_bsh_actor        ON booking_status_history (actor_user_id);
CREATE INDEX idx_bsh_transitioned ON booking_status_history (transitioned_at DESC);


-- ============================================================
-- TABLE: invoices
-- One invoice per corporate client per billing cycle
-- ============================================================

CREATE TABLE invoices (
  id                    UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id             UUID            NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
  corporate_client_id   UUID            NOT NULL REFERENCES corporate_clients(id) ON DELETE RESTRICT,
  invoice_number        TEXT            NOT NULL,
  billing_period_start  DATE            NOT NULL,
  billing_period_end    DATE            NOT NULL,
  subtotal              NUMERIC(12, 2)  NOT NULL DEFAULT 0,
  cgst_amount           NUMERIC(10, 2)  NOT NULL DEFAULT 0,
  sgst_amount           NUMERIC(10, 2)  NOT NULL DEFAULT 0,
  total_amount          NUMERIC(12, 2)  NOT NULL DEFAULT 0,
  cancellation_fees     NUMERIC(10, 2)  NOT NULL DEFAULT 0,
  status                TEXT            NOT NULL DEFAULT 'DRAFT',
  due_date              DATE,
  sent_at               TIMESTAMPTZ,
  paid_at               TIMESTAMPTZ,
  payment_mode          TEXT,
  payment_reference     TEXT,
  notes                 TEXT,
  created_at            TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
  updated_at            TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

  CONSTRAINT uq_invoices_number_tenant UNIQUE (tenant_id, invoice_number),
  CONSTRAINT chk_invoice_period CHECK (billing_period_end >= billing_period_start)
);

CREATE INDEX idx_invoices_tenant_id           ON invoices (tenant_id);
CREATE INDEX idx_invoices_corporate_client_id ON invoices (corporate_client_id);
CREATE INDEX idx_invoices_status              ON invoices (status);
CREATE INDEX idx_invoices_billing_period      ON invoices (billing_period_start, billing_period_end);

CREATE TRIGGER trg_invoices_updated_at
  BEFORE UPDATE ON invoices
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ============================================================
-- TABLE: invoice_line_items
-- One row per completed booking included in an invoice
-- ============================================================

CREATE TABLE invoice_line_items (
  id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id       UUID            NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
  invoice_id      UUID            NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
  booking_id      UUID            NOT NULL REFERENCES bookings(id) ON DELETE RESTRICT,
  description     TEXT            NOT NULL,
  trip_date       DATE            NOT NULL,
  vehicle_type    TEXT            NOT NULL,
  base_fare       NUMERIC(10, 2)  NOT NULL DEFAULT 0,
  cgst_amount     NUMERIC(10, 2)  NOT NULL DEFAULT 0,
  sgst_amount     NUMERIC(10, 2)  NOT NULL DEFAULT 0,
  line_total      NUMERIC(10, 2)  NOT NULL DEFAULT 0,
  created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

  CONSTRAINT uq_line_items_booking_invoice UNIQUE (invoice_id, booking_id)
);

CREATE INDEX idx_invoice_line_items_invoice_id  ON invoice_line_items (invoice_id);
CREATE INDEX idx_invoice_line_items_booking_id  ON invoice_line_items (booking_id);
CREATE INDEX idx_invoice_line_items_tenant_id   ON invoice_line_items (tenant_id);

CREATE TRIGGER trg_invoice_line_items_updated_at
  BEFORE UPDATE ON invoice_line_items
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ============================================================
-- TABLE: notifications_log
-- Outbound notification audit log
-- ============================================================

CREATE TABLE notifications_log (
  id                UUID                    PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id         UUID                    NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
  booking_id        UUID                    REFERENCES bookings(id) ON DELETE SET NULL,
  recipient_user_id UUID                    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  channel           TEXT                    NOT NULL,
  status            TEXT                    NOT NULL DEFAULT 'PENDING',
  event_type        TEXT                    NOT NULL,
  subject           TEXT,
  body              TEXT,
  sent_at           TIMESTAMPTZ,
  failure_reason    TEXT,
  created_at        TIMESTAMPTZ             NOT NULL DEFAULT NOW(),
  updated_at        TIMESTAMPTZ             NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_log_tenant_id         ON notifications_log (tenant_id);
CREATE INDEX idx_notifications_log_booking_id        ON notifications_log (booking_id);
CREATE INDEX idx_notifications_log_recipient_user_id ON notifications_log (recipient_user_id);
CREATE INDEX idx_notifications_log_status            ON notifications_log (status);
CREATE INDEX idx_notifications_log_channel           ON notifications_log (channel);

CREATE TRIGGER trg_notifications_log_updated_at
  BEFORE UPDATE ON notifications_log
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();
