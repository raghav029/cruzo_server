package com.carbooking.service;

import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.dto.response.public_.PublicVehicleDetailResponse;
import com.carbooking.dto.response.public_.PublicVehicleResponse;
import com.carbooking.dto.response.vehicle.VehiclePackageResponse;
import com.carbooking.entity.Vehicle;
import com.carbooking.entity.VehiclePackage;
import com.carbooking.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicVehicleService {

    private final VehicleRepository vehicleRepo;

    public List<PublicVehicleResponse> listPublished(UUID tenantId, UUID cityId) {
        return vehicleRepo.findPublishedByTenantAndCity(tenantId, cityId)
            .stream().map(this::toCard).collect(Collectors.toList());
    }

    public PublicVehicleDetailResponse getDetail(UUID vehicleId, UUID tenantId) {
        Vehicle v = vehicleRepo.findByIdAndTenantIdAndPublishedTrue(vehicleId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        return toDetail(v);
    }

    private PublicVehicleResponse toCard(Vehicle v) {
        BigDecimal startingPrice = v.getPackages().stream()
            .filter(VehiclePackage::isActive)
            .map(VehiclePackage::getBaseRental)
            .min(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);

        String thumbnail = v.getImages().isEmpty() ? null
            : v.getImages().get(0).getImageUrl();

        return PublicVehicleResponse.builder()
            .id(v.getId()).make(v.getMake()).model(v.getModel())
            .color(v.getColor()).vehicleType(v.getVehicleType())
            .category(v.getCategory()).bagCapacity(v.getBagCapacity())
            .thumbnailUrl(thumbnail).startingFromPrice(startingPrice)
            .build();
    }

    private PublicVehicleDetailResponse toDetail(Vehicle v) {
        List<String> imageUrls = v.getImages().stream()
            .map(i -> i.getImageUrl()).collect(Collectors.toList());

        List<VehiclePackageResponse> packages = v.getPackages().stream()
            .filter(VehiclePackage::isActive)
            .map(this::toPackageResponse)
            .collect(Collectors.toList());

        return PublicVehicleDetailResponse.builder()
            .id(v.getId()).make(v.getMake()).model(v.getModel())
            .color(v.getColor()).vehicleType(v.getVehicleType())
            .category(v.getCategory()).bagCapacity(v.getBagCapacity())
            .description(v.getDescription()).amenities(v.getAmenities())
            .imageUrls(imageUrls).packages(packages)
            .build();
    }

    private VehiclePackageResponse toPackageResponse(VehiclePackage p) {
        return VehiclePackageResponse.builder()
            .id(p.getId()).name(p.getName()).baseRental(p.getBaseRental())
            .includedKm(p.getIncludedKm()).includedHours(p.getIncludedHours())
            .extraPerKm(p.getExtraPerKm()).extraPerHour(p.getExtraPerHour())
            .driveBatta(p.getDriveBatta()).outstationBatta(p.getOutstationBatta())
            .nightBatta(p.getNightBatta()).build();
    }
}
