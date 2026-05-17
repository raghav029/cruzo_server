package com.carbooking.modules.booking.application;

import com.carbooking.common.enums.BookingStatus;
import com.carbooking.common.enums.Role;
import com.carbooking.entity.Booking;
import com.carbooking.entity.Driver;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.User;
import com.carbooking.modules.booking.domain.port.BookingPort;
import com.carbooking.modules.booking.dto.response.BookingResponse;
import com.carbooking.modules.fleet.domain.port.DriverPort;
import com.carbooking.modules.notification.application.NotificationService;
import com.carbooking.repository.UserRepository;
import com.carbooking.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingDriverStatusServiceTest {

    @Mock
    private BookingPort bookingRepository;

    @Mock
    private DriverPort driverRepository;

    @Mock
    private BookingHistoryHelper historyHelper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private UserRepository userRepository;

    private BookingDriverStatusService service;

    private UUID tenantId;
    private UUID userId;
    private UUID driverId;

    @BeforeEach
    void setUp() {
        service = new BookingDriverStatusService(
                bookingRepository,
                driverRepository,
                historyHelper,
                notificationService,
                bookingMapper);
        ReflectionTestUtils.setField(service, "userRepository", userRepository);

        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        driverId = UUID.randomUUID();

        UserPrincipal principal = new UserPrincipal(
                userId,
                tenantId,
                "driver@example.com",
                "password",
                Role.DRIVER.name(),
                true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateDriverStatus_setsOtpGeneratedAtWhenDriverArrives() {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);

        User driverUser = new User();
        driverUser.setId(userId);

        Driver driver = new Driver();
        driver.setId(driverId);
        driver.setUser(driverUser);
        driver.setTenant(tenant);

        Booking booking = new Booking();
        booking.setTenant(tenant);
        booking.setStatus(BookingStatus.DRIVER_EN_ROUTE);
        booking.setDriver(driver);
        booking.setOtpGeneratedAt(Instant.now().minusSeconds(900));

        when(bookingRepository.findById(any(UUID.class))).thenReturn(Optional.of(booking));
        when(driverRepository.findByUser(any(User.class))).thenReturn(Optional.of(driver));
        when(userRepository.findById(userId)).thenReturn(Optional.of(driverUser));
        BookingResponse mappedResponse = BookingResponse.builder().build();
        when(bookingMapper.toResponse(booking)).thenReturn(mappedResponse);

        BookingResponse response = service.updateDriverStatus(UUID.randomUUID(), "ARRIVED");

        assertSame(mappedResponse, response);
        assertNotNull(booking.getOtpGeneratedAt());
        verify(notificationService).notifyDriverEnRoute(booking);
    }
}
