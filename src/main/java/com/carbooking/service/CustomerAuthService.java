package com.carbooking.service;

import com.carbooking.common.exception.BusinessRuleException;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.dto.request.b2c.SendOtpRequest;
import com.carbooking.dto.request.b2c.UpdateProfileRequest;
import com.carbooking.dto.request.b2c.VerifyOtpRequest;
import com.carbooking.dto.response.b2c.B2CAuthResponse;
import com.carbooking.dto.response.b2c.CustomerResponse;
import com.carbooking.dto.response.b2c.OtpResponse;
import com.carbooking.entity.Customer;
import com.carbooking.entity.Tenant;
import com.carbooking.repository.CustomerRepository;
import com.carbooking.repository.TenantRepository;
import com.carbooking.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerAuthService {

    private static final int OTP_EXPIRY_MINUTES = 10;

    private final CustomerRepository customerRepo;
    private final TenantRepository tenantRepo;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate;

    @Value("${msg91.auth-key:}")
    private String msg91AuthKey;

    private final SecureRandom random = new SecureRandom();

    @Transactional
    public OtpResponse sendOtp(SendOtpRequest req) {
        Tenant tenant = resolveTenant(req.getTenantId());
        if (!tenant.supportsB2C())
            throw new BusinessRuleException("Tenant does not support B2C");

        Customer customer = customerRepo
            .findByPhoneAndTenantId(req.getPhone(), tenant.getId())
            .orElseGet(() -> Customer.builder()
                .tenant(tenant).phone(req.getPhone()).name(req.getName()).build());

        customer.setName(req.getName());
        customer.setOtpCode(generateOtp());
        customer.setOtpExpiresAt(Instant.now().plus(OTP_EXPIRY_MINUTES, ChronoUnit.MINUTES));
        customerRepo.save(customer);

        sendSms(req.getPhone(), customer.getOtpCode());

        return OtpResponse.builder()
            .maskedPhone(mask(req.getPhone()))
            .expiresInSeconds(OTP_EXPIRY_MINUTES * 60)
            .build();
    }

    @Transactional
    public B2CAuthResponse verifyOtp(VerifyOtpRequest req) {
        Tenant tenant = resolveTenant(req.getTenantId());

        Customer customer = customerRepo
            .findByPhoneAndTenantId(req.getPhone(), tenant.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Send OTP first"));

        if (!req.getOtp().equals(customer.getOtpCode()))
            throw new BusinessRuleException("Invalid OTP");

        if (customer.getOtpExpiresAt() == null || Instant.now().isAfter(customer.getOtpExpiresAt()))
            throw new BusinessRuleException("OTP expired");

        customer.setOtpCode(null);
        customer.setOtpExpiresAt(null);
        customer.setVerified(true);
        customerRepo.save(customer);

        String token = jwtTokenProvider.generateToken(
            customer.getId(), tenant.getId(), "CUSTOMER", null);

        return B2CAuthResponse.builder()
            .token(token)
            .customer(toResponse(customer))
            .build();
    }

    public CustomerResponse getProfile(UUID customerId) {
        return toResponse(customerRepo.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found")));
    }

    @Transactional
    public CustomerResponse updateProfile(UUID customerId, UpdateProfileRequest req) {
        Customer customer = customerRepo.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        customer.setName(req.getName());
        if (req.getEmail() != null) customer.setEmail(req.getEmail());
        return toResponse(customerRepo.save(customer));
    }

    private Tenant resolveTenant(UUID tenantId) {
        return tenantRepo.findById(tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
    }

    private String generateOtp() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    private String mask(String phone) {
        return phone.substring(0, 2) + "XXXXXX" + phone.substring(8);
    }

    private void sendSms(String phone, String otp) {
        if (msg91AuthKey == null || msg91AuthKey.isBlank()) {
            log.info("MSG91 key not set — OTP for +91{} is {} [DEV]", phone, otp);
            return;
        }
        try {
            String url = "https://api.msg91.com/api/v5/otp"
                + "?authkey={key}&mobile={mobile}&otp={otp}"
                + "&message=Your+OTP+is+{otp}.+Valid+10+minutes.&sender=CARBKG";
            restTemplate.getForObject(url, String.class,
                Map.of("key", msg91AuthKey, "mobile", "91" + phone, "otp", otp));
        } catch (Exception e) {
            log.warn("SMS failed for {}: {}", phone, e.getMessage());
        }
    }

    private CustomerResponse toResponse(Customer c) {
        return CustomerResponse.builder()
            .id(c.getId()).name(c.getName()).phone(c.getPhone())
            .email(c.getEmail()).verified(c.isVerified())
            .createdAt(c.getCreatedAt()).build();
    }
}
