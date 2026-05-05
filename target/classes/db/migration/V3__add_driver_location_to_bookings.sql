ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS driver_current_lat  DECIMAL(9,6),
    ADD COLUMN IF NOT EXISTS driver_current_lng  DECIMAL(9,6),
    ADD COLUMN IF NOT EXISTS location_updated_at TIMESTAMPTZ;
