package com.carbooking.modules.promo.application;

import com.carbooking.common.exception.BusinessRuleException;
import com.carbooking.common.exception.DuplicateResourceException;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.util.TenantSupport;
import com.carbooking.entity.PromoCode;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.enums.DiscountType;
import com.carbooking.modules.promo.domain.port.PromoCodePort;
import com.carbooking.modules.promo.dto.request.CreatePromoCodeRequest;
import com.carbooking.modules.promo.dto.request.ValidatePromoRequest;
import com.carbooking.modules.promo.dto.response.PromoCodeResponse;
import com.carbooking.modules.promo.dto.response.ValidatePromoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Service
public class PromoCodeService extends TenantSupport {

    private final PromoCodePort promoCodeRepository;

    public PromoCodeService(PromoCodePort promoCodeRepository) {
        this.promoCodeRepository = promoCodeRepository;
    }

    @Transactional
    public PromoCodeResponse create(CreatePromoCodeRequest request) {
        Tenant tenant = requireTenant();

        if (promoCodeRepository.existsByTenantAndCodeIgnoreCase(tenant, request.getCode())) {
            throw new DuplicateResourceException("Promo code '" + request.getCode() + "' already exists");
        }

        if (request.getDiscountType() == DiscountType.PERCENTAGE
                && request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessRuleException("Percentage discount cannot exceed 100");
        }

        PromoCode promo = PromoCode.builder()
                .tenant(tenant)
                .code(request.getCode().toUpperCase())
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minBookingValue(request.getMinBookingValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .maxUses(request.getMaxUses())
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .active(true)
                .build();

        return toResponse(promoCodeRepository.save(promo));
    }

    public Page<PromoCodeResponse> list(Pageable pageable) {
        Tenant tenant = requireTenant();
        return promoCodeRepository.findByTenant(tenant, pageable).map(this::toResponse);
    }

    public PromoCodeResponse get(UUID id) {
        PromoCode promo = find(id);
        assertSameTenant(promo.getTenant().getId());
        return toResponse(promo);
    }

    @Transactional
    public PromoCodeResponse deactivate(UUID id) {
        PromoCode promo = find(id);
        assertSameTenant(promo.getTenant().getId());
        promo.setActive(false);
        return toResponse(promoCodeRepository.save(promo));
    }

    /** Called by B2CBookingService when applying promo at booking creation. */
    @Transactional
    public BigDecimal applyPromo(Tenant tenant, String code, BigDecimal bookingAmount) {
        PromoCode promo = promoCodeRepository.findByTenantAndCodeIgnoreCase(tenant, code)
                .orElseThrow(() -> new ResourceNotFoundException("Promo code not found"));

        validatePromo(promo, bookingAmount);

        BigDecimal discount = computeDiscount(promo, bookingAmount);
        promo.setUsedCount(promo.getUsedCount() + 1);
        promoCodeRepository.save(promo);
        return discount;
    }

    /** Stateless validate — does NOT increment used_count. Used by validate endpoint. */
    public ValidatePromoResponse validate(ValidatePromoRequest request) {
        Tenant tenant = requireTenant();
        PromoCode promo = promoCodeRepository.findByTenantAndCodeIgnoreCase(tenant, request.getCode())
                .orElseThrow(() -> new ResourceNotFoundException("Promo code not found"));

        validatePromo(promo, request.getBookingAmount());

        BigDecimal discount = computeDiscount(promo, request.getBookingAmount());
        return ValidatePromoResponse.builder()
                .code(promo.getCode())
                .discountAmount(discount)
                .finalAmount(request.getBookingAmount().subtract(discount))
                .build();
    }

    private void validatePromo(PromoCode promo, BigDecimal bookingAmount) {
        if (!promo.isActive()) {
            throw new BusinessRuleException("Promo code is no longer active");
        }
        Instant now = Instant.now();
        if (now.isBefore(promo.getValidFrom())) {
            throw new BusinessRuleException("Promo code is not yet valid");
        }
        if (promo.getValidUntil() != null && now.isAfter(promo.getValidUntil())) {
            throw new BusinessRuleException("Promo code has expired");
        }
        if (promo.getMaxUses() != null && promo.getUsedCount() >= promo.getMaxUses()) {
            throw new BusinessRuleException("Promo code usage limit reached");
        }
        if (promo.getMinBookingValue() != null && bookingAmount.compareTo(promo.getMinBookingValue()) < 0) {
            throw new BusinessRuleException("Minimum booking amount of ₹"
                    + promo.getMinBookingValue().setScale(0, RoundingMode.HALF_UP) + " required for this promo");
        }
    }

    private BigDecimal computeDiscount(PromoCode promo, BigDecimal bookingAmount) {
        BigDecimal discount;
        if (promo.getDiscountType() == DiscountType.FLAT) {
            discount = promo.getDiscountValue().min(bookingAmount);
        } else {
            discount = bookingAmount.multiply(promo.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (promo.getMaxDiscountAmount() != null) {
                discount = discount.min(promo.getMaxDiscountAmount());
            }
        }
        return discount;
    }

    private PromoCode find(UUID id) {
        return promoCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promo code not found"));
    }

    private PromoCodeResponse toResponse(PromoCode p) {
        return PromoCodeResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .description(p.getDescription())
                .discountType(p.getDiscountType())
                .discountValue(p.getDiscountValue())
                .minBookingValue(p.getMinBookingValue())
                .maxDiscountAmount(p.getMaxDiscountAmount())
                .maxUses(p.getMaxUses())
                .usedCount(p.getUsedCount())
                .validFrom(p.getValidFrom())
                .validUntil(p.getValidUntil())
                .active(p.isActive())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
