package com.carbooking.service;

import com.carbooking.entity.VehiclePackage;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class B2CPricingEngine {

    private static final BigDecimal GST_RATE = new BigDecimal("0.18");

    public BigDecimal estimateFare(VehiclePackage pkg) {
        BigDecimal gst = pkg.getBaseRental()
            .multiply(GST_RATE)
            .setScale(2, RoundingMode.HALF_UP);
        return pkg.getBaseRental().add(gst);
    }

    public FareBreakdown calculateFinalFare(
            VehiclePackage pkg,
            BigDecimal extraKm,
            BigDecimal extraHours,
            boolean applyDriveBatta,
            boolean applyOutstationBatta,
            boolean applyNightBatta,
            BigDecimal parkingFee,
            BigDecimal tollFee) {

        BigDecimal extraKmCharge   = multiply(extraKm, pkg.getExtraPerKm());
        BigDecimal extraHourCharge = multiply(extraHours, pkg.getExtraPerHour());
        BigDecimal driveBatta      = applyDriveBatta      ? pkg.getDriveBatta()      : BigDecimal.ZERO;
        BigDecimal outstationBatta = applyOutstationBatta ? pkg.getOutstationBatta() : BigDecimal.ZERO;
        BigDecimal nightBatta      = applyNightBatta      ? pkg.getNightBatta()      : BigDecimal.ZERO;
        BigDecimal parking         = parkingFee != null   ? parkingFee               : BigDecimal.ZERO;
        BigDecimal toll            = tollFee != null      ? tollFee                  : BigDecimal.ZERO;

        BigDecimal subtotal = pkg.getBaseRental()
            .add(extraKmCharge).add(extraHourCharge)
            .add(driveBatta).add(outstationBatta).add(nightBatta)
            .add(parking).add(toll);

        BigDecimal gst   = subtotal.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(gst);

        return new FareBreakdown(
            extraKmCharge, extraHourCharge,
            driveBatta, outstationBatta, nightBatta,
            parking, toll, gst, total);
    }

    public BigDecimal cancellationCharge(BigDecimal estimatedFare, long hoursUntilTrip) {
        if (hoursUntilTrip >= 72) return BigDecimal.ZERO;
        if (hoursUntilTrip >= 48) return estimatedFare
            .multiply(new BigDecimal("0.5"))
            .setScale(2, RoundingMode.HALF_UP);
        return estimatedFare;
    }

    private BigDecimal multiply(BigDecimal qty, BigDecimal rate) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return qty.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    public record FareBreakdown(
        BigDecimal extraKmCharge,
        BigDecimal extraHourCharge,
        BigDecimal driveBattaApplied,
        BigDecimal outstationBattaApplied,
        BigDecimal nightBattaApplied,
        BigDecimal parkingFee,
        BigDecimal tollFee,
        BigDecimal gstAmount,
        BigDecimal totalFare
    ) {}
}
