-- V9: Add boarding_otp to bookings for two-OTP pickup+drop flow
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS boarding_otp VARCHAR(6);
