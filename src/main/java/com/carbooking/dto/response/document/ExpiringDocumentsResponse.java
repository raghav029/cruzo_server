package com.carbooking.dto.response.document;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ExpiringDocumentsResponse {
    private List<DriverExpiryItem> expiringLicenses;
    private List<DriverExpiryItem> expiringDriverInsurance;
    private List<VehicleExpiryItem> expiringVehicleInsurance;
    private List<VehicleExpiryItem> expiringFitnessCerts;
}
