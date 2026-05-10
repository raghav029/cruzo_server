-- Seed historical bookings and daily trips for dashboard charts
-- Uses existence checks — safe to run on a fresh DB after V1-V7

DO $$
DECLARE
  v_tenant_id       UUID;
  v_employee_id     UUID;
  v_driver_id       UUID;
  v_vehicle_id      UUID;
  v_client_id       UUID;
  v_schedule_id     UUID;

  -- hourly distributions: index = offset from 6am (0=6am..15=9pm)
  hour_weights INT[] := ARRAY[2,8,18,22,14,9,11,12,9,7,10,15,22,17,11,5];
  day_offset   INT;
  h_idx        INT;
  h_val        INT;
  trips_this_day INT;
  booking_hour INT;
  fare         NUMERIC;
  sched_at     TIMESTAMPTZ;
  completed_at TIMESTAMPTZ;
BEGIN

  SELECT id INTO v_tenant_id FROM tenants LIMIT 1;
  IF v_tenant_id IS NULL THEN RETURN; END IF;

  SELECT u.id INTO v_employee_id
  FROM users u WHERE u.tenant_id = v_tenant_id AND u.role = 'EMPLOYEE' LIMIT 1;
  IF v_employee_id IS NULL THEN RETURN; END IF;

  SELECT d.id INTO v_driver_id
  FROM drivers d WHERE d.tenant_id = v_tenant_id LIMIT 1;

  SELECT v.id INTO v_vehicle_id
  FROM vehicles v WHERE v.tenant_id = v_tenant_id LIMIT 1;

  SELECT cc.id INTO v_client_id
  FROM corporate_clients cc WHERE cc.tenant_id = v_tenant_id LIMIT 1;

  -- Insert historical bookings for last 30 days (weighted by hour)
  FOR day_offset IN 1..30 LOOP
    -- trips per day varies realistically (15-35)
    trips_this_day := 15 + (day_offset * 7 % 21);

    FOR h_idx IN 1..trips_this_day LOOP
      -- pick an hour bucket weighted by hour_weights
      booking_hour := 6 + (h_idx * 3 % 16); -- rough spread 6-21
      fare := (200 + (h_idx * 37 % 800))::NUMERIC;
      sched_at := (CURRENT_DATE - day_offset + INTERVAL '1 day' + make_interval(hours => booking_hour))::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata';
      completed_at := sched_at + INTERVAL '90 minutes';

      IF v_client_id IS NOT NULL THEN
        INSERT INTO bookings (
          id, tenant_id, employee_id, corporate_client_id,
          pickup_address, drop_address,
          scheduled_at, status,
          final_fare, trip_completed_at,
          created_at, updated_at
        ) VALUES (
          gen_random_uuid(), v_tenant_id, v_employee_id, v_client_id,
          'Main Gate, Andheri West, Mumbai',
          'Office Park, BKC, Mumbai',
          sched_at, 'COMPLETED',
          fare, completed_at,
          sched_at - INTERVAL '2 hours', sched_at - INTERVAL '2 hours'
        );
      END IF;
    END LOOP;
  END LOOP;

  -- Insert booking status history for each seeded booking
  INSERT INTO booking_status_history (id, booking_id, status, changed_at)
  SELECT gen_random_uuid(), b.id, 'COMPLETED', b.trip_completed_at
  FROM bookings b
  WHERE b.tenant_id = v_tenant_id
    AND b.status = 'COMPLETED'
    AND NOT EXISTS (
      SELECT 1 FROM booking_status_history bsh WHERE bsh.booking_id = b.id
    );

  -- Insert daily_trips for last 13 days (2-5 trips per day)
  SELECT ds.id INTO v_schedule_id
  FROM daily_schedules ds WHERE ds.tenant_id = v_tenant_id LIMIT 1;

  IF v_schedule_id IS NOT NULL THEN
    FOR day_offset IN 0..12 LOOP
      trips_this_day := 2 + (day_offset % 4);
      FOR h_idx IN 1..trips_this_day LOOP
        -- skip if already exists for this date+schedule combo
        INSERT INTO daily_trips (
          id, tenant_id, daily_schedule_id,
          trip_date, status,
          boarding_otp, drop_otp,
          driver_alert_sent,
          created_at, updated_at
        )
        SELECT
          gen_random_uuid(), v_tenant_id, v_schedule_id,
          CURRENT_DATE - day_offset,
          CASE WHEN day_offset > 0 THEN 'COMPLETED' ELSE 'SCHEDULED' END,
          LPAD((1000 + h_idx * 17 % 9000)::text, 4, '0'),
          LPAD((5000 + h_idx * 31 % 4999)::text, 4, '0'),
          true,
          NOW() - make_interval(days => day_offset),
          NOW() - make_interval(days => day_offset)
        WHERE NOT EXISTS (
          SELECT 1 FROM daily_trips dt2
          WHERE dt2.tenant_id = v_tenant_id
            AND dt2.daily_schedule_id = v_schedule_id
            AND dt2.trip_date = CURRENT_DATE - day_offset
        );
      END LOOP;
    END LOOP;
  END IF;

END $$;
