package com.carbooking.modules.document.application;

import com.carbooking.common.enums.VehicleStatus;
import com.carbooking.common.util.TenantSupport;
import com.carbooking.entity.Driver;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.Vehicle;
import com.carbooking.modules.document.dto.response.DriverExpiryItem;
import com.carbooking.modules.document.dto.response.ExpiringDocumentsResponse;
import com.carbooking.modules.document.dto.response.VehicleExpiryItem;
import com.carbooking.modules.fleet.domain.port.DriverPort;
import com.carbooking.modules.fleet.domain.port.VehiclePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentExpiryService extends TenantSupport {

    private final DriverPort driverRepository;
    private final VehiclePort vehicleRepository;

    public DocumentExpiryService(DriverPort driverRepository, VehiclePort vehicleRepository) {
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional(readOnly = true)
    public ExpiringDocumentsResponse getExpiringDocuments() {
        Tenant tenant = requireTenant();
        LocalDate threshold = LocalDate.now().plusDays(30);

        List<Driver> expiringLicenseDrivers = driverRepository.findByTenantAndLicenseExpiryBefore(tenant, threshold);
        List<Driver> expiringInsuranceDrivers = driverRepository.findByTenantAndInsuranceExpiryBefore(tenant, threshold);
        List<Vehicle> vehiclesExpiringInsurance = vehicleRepository.findByTenantAndInsuranceExpiryBeforeAndStatusNot(
                tenant, threshold, VehicleStatus.INACTIVE);
        List<Vehicle> vehiclesExpiringFitness = vehicleRepository.findByTenantAndFitnessExpiryBeforeAndStatusNot(
                tenant, threshold, VehicleStatus.INACTIVE);

        return ExpiringDocumentsResponse.builder()
                .expiringLicenses(expiringLicenseDrivers.stream()
                        .map(d -> toDriverExpiryItem(d, d.getLicenseExpiry())).collect(Collectors.toList()))
                .expiringDriverInsurance(expiringInsuranceDrivers.stream()
                        .filter(d -> d.getInsuranceExpiry() != null)
                        .map(d -> toDriverExpiryItem(d, d.getInsuranceExpiry())).collect(Collectors.toList()))
                .expiringVehicleInsurance(vehiclesExpiringInsurance.stream()
                        .filter(v -> v.getInsuranceExpiry() != null)
                        .map(v -> toVehicleExpiryItem(v, v.getInsuranceExpiry())).collect(Collectors.toList()))
                .expiringFitnessCerts(vehiclesExpiringFitness.stream()
                        .filter(v -> v.getFitnessExpiry() != null)
                        .map(v -> toVehicleExpiryItem(v, v.getFitnessExpiry())).collect(Collectors.toList()))
                .build();
    }

    private DriverExpiryItem toDriverExpiryItem(Driver d, LocalDate expiryDate) {
        return DriverExpiryItem.builder()
                .driverId(d.getId())
                .driverName(d.getUser().getFullName())
                .phone(d.getUser().getPhone())
                .expiryDate(expiryDate)
                .daysUntilExpiry(ChronoUnit.DAYS.between(LocalDate.now(), expiryDate))
                .build();
    }

    private VehicleExpiryItem toVehicleExpiryItem(Vehicle v, LocalDate expiryDate) {
        return VehicleExpiryItem.builder()
                .vehicleId(v.getId())
                .plateNumber(v.getPlateNumber())
                .make(v.getMake())
                .model(v.getModel())
                .expiryDate(expiryDate)
                .daysUntilExpiry(ChronoUnit.DAYS.between(LocalDate.now(), expiryDate))
                .build();
    }
}
