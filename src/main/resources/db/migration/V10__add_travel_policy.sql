ALTER TABLE corporate_clients
    ADD COLUMN max_booking_value     DECIMAL(12,2) NULL,
    ADD COLUMN allowed_vehicle_types TEXT          NULL;

ALTER TABLE corporate_employees
    ADD COLUMN max_booking_value_override     DECIMAL(12,2) NULL,
    ADD COLUMN allowed_vehicle_types_override TEXT          NULL;
