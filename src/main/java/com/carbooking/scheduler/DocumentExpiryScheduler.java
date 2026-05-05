package com.carbooking.scheduler;
import com.carbooking.modules.notification.application.NotificationService;

import com.carbooking.common.enums.Role;
import com.carbooking.common.enums.VehicleStatus;
import com.carbooking.entity.Driver;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.User;
import com.carbooking.entity.Vehicle;
import com.carbooking.repository.DriverRepository;
import com.carbooking.repository.TenantRepository;
import com.carbooking.repository.UserRepository;
import com.carbooking.repository.VehicleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
public class DocumentExpiryScheduler {

    private final TenantRepository tenantRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public DocumentExpiryScheduler(TenantRepository tenantRepository,
                                    DriverRepository driverRepository,
                                    VehicleRepository vehicleRepository,
                                    UserRepository userRepository,
                                    @Lazy NotificationService notificationService) {
        this.tenantRepository = tenantRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void checkAndAlertExpiringDocuments() {
        LocalDate threshold = LocalDate.now().plusDays(30);
        List<Tenant> tenants = tenantRepository.findAll();

        for (Tenant tenant : tenants) {
            try {
                List<User> fleetManagers = userRepository.findByTenantAndRole(tenant, Role.FLEET_MANAGER);
                if (fleetManagers.isEmpty()) continue;

                // Check driver licenses
                List<Driver> expiringLicenses = driverRepository.findByTenantAndLicenseExpiryBefore(tenant, threshold);
                for (Driver driver : expiringLicenses) {
                    if (driver.getLastExpiryAlertSentAt() != null &&
                            driver.getLastExpiryAlertSentAt().isAfter(Instant.now().minus(20, ChronoUnit.HOURS))) {
                        continue;
                    }
                    long days = ChronoUnit.DAYS.between(LocalDate.now(), driver.getLicenseExpiry());
                    String msg = String.format(
                            "⚠️ Document expiry alert: Driver %s's license expires on %s (%d days). Please renew.",
                            driver.getUser().getFullName(), driver.getLicenseExpiry(), days);
                    for (User fm : fleetManagers) {
                        notificationService.sendDirectSms(tenant, fm, "LICENSE_EXPIRY", msg);
                    }
                    driver.setLastExpiryAlertSentAt(Instant.now());
                    driverRepository.save(driver);
                }

                // Check driver insurance
                List<Driver> expiringInsurance = driverRepository.findByTenantAndInsuranceExpiryBefore(tenant, threshold);
                for (Driver driver : expiringInsurance) {
                    if (driver.getInsuranceExpiry() == null) continue;
                    if (driver.getLastExpiryAlertSentAt() != null &&
                            driver.getLastExpiryAlertSentAt().isAfter(Instant.now().minus(20, ChronoUnit.HOURS))) {
                        continue;
                    }
                    long days = ChronoUnit.DAYS.between(LocalDate.now(), driver.getInsuranceExpiry());
                    String msg = String.format(
                            "⚠️ Document expiry alert: Driver %s's insurance expires on %s (%d days). Please renew.",
                            driver.getUser().getFullName(), driver.getInsuranceExpiry(), days);
                    for (User fm : fleetManagers) {
                        notificationService.sendDirectSms(tenant, fm, "DRIVER_INSURANCE_EXPIRY", msg);
                    }
                }

                // Check vehicle insurance
                List<Vehicle> vehiclesInsurance = vehicleRepository.findByTenantAndInsuranceExpiryBeforeAndStatusNot(
                        tenant, threshold, VehicleStatus.INACTIVE);
                for (Vehicle vehicle : vehiclesInsurance) {
                    if (vehicle.getInsuranceExpiry() == null) continue;
                    if (vehicle.getLastExpiryAlertSentAt() != null &&
                            vehicle.getLastExpiryAlertSentAt().isAfter(Instant.now().minus(20, ChronoUnit.HOURS))) {
                        continue;
                    }
                    long days = ChronoUnit.DAYS.between(LocalDate.now(), vehicle.getInsuranceExpiry());
                    String msg = String.format(
                            "⚠️ Document expiry alert: Vehicle %s insurance expires on %s (%d days). Please renew.",
                            vehicle.getPlateNumber(), vehicle.getInsuranceExpiry(), days);
                    for (User fm : fleetManagers) {
                        notificationService.sendDirectSms(tenant, fm, "VEHICLE_INSURANCE_EXPIRY", msg);
                    }
                    vehicle.setLastExpiryAlertSentAt(Instant.now());
                    vehicleRepository.save(vehicle);
                }

                // Check vehicle fitness
                List<Vehicle> vehiclesFitness = vehicleRepository.findByTenantAndFitnessExpiryBeforeAndStatusNot(
                        tenant, threshold, VehicleStatus.INACTIVE);
                for (Vehicle vehicle : vehiclesFitness) {
                    if (vehicle.getFitnessExpiry() == null) continue;
                    if (vehicle.getLastExpiryAlertSentAt() != null &&
                            vehicle.getLastExpiryAlertSentAt().isAfter(Instant.now().minus(20, ChronoUnit.HOURS))) {
                        continue;
                    }
                    long days = ChronoUnit.DAYS.between(LocalDate.now(), vehicle.getFitnessExpiry());
                    String msg = String.format(
                            "⚠️ Document expiry alert: Vehicle %s fitness certificate expires on %s (%d days). Please renew.",
                            vehicle.getPlateNumber(), vehicle.getFitnessExpiry(), days);
                    for (User fm : fleetManagers) {
                        notificationService.sendDirectSms(tenant, fm, "FITNESS_CERT_EXPIRY", msg);
                    }
                    vehicle.setLastExpiryAlertSentAt(Instant.now());
                    vehicleRepository.save(vehicle);
                }

            } catch (Exception e) {
                log.error("Document expiry check failed for tenant {}: {}", tenant.getId(), e.getMessage());
            }
        }
    }
}
