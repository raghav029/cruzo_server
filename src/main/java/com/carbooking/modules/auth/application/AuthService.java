package com.carbooking.modules.auth.application;

import com.carbooking.common.enums.UserStatus;
import com.carbooking.common.exception.BusinessRuleException;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.util.SecurityUtils;
import com.carbooking.entity.User;
import com.carbooking.modules.auth.dto.request.ChangePasswordRequest;
import com.carbooking.modules.auth.dto.request.ForgotPasswordRequest;
import com.carbooking.modules.auth.dto.request.LoginRequest;
import com.carbooking.modules.auth.dto.response.LoginResponse;
import com.carbooking.modules.notification.application.NotificationService;
import com.carbooking.repository.UserRepository;
import com.carbooking.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final NotificationService notificationService;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       @Lazy NotificationService notificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.notificationService = notificationService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (user.getStatus() == UserStatus.INACTIVE || user.getStatus() == UserStatus.SUSPENDED) {
            throw new BusinessRuleException("Your account is " + user.getStatus().name().toLowerCase() + ". Please contact support.");
        }

        if (user.getTenant() != null && !user.getTenant().isActive()) {
            throw new BusinessRuleException("Your organisation account has been suspended. Please contact support.");
        }

        String token = jwtTokenProvider.generateToken(
                user.getId(),
                user.getTenant() != null ? user.getTenant().getId() : null,
                user.getRole().name()
        );

        user.setLastLoginAt(Instant.now());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .role(user.getRole().name())
                .tenantId(user.getTenant() != null ? user.getTenant().getId() : null)
                .fullName(user.getFullName())
                .email(user.getEmail())
                .expiresAt(Instant.now().plusMillis(jwtExpirationMs))
                .build();
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String tempPassword = "Tmp@" + UUID.randomUUID().toString().substring(0, 8);
            user.setPasswordHash(passwordEncoder.encode(tempPassword));
            userRepository.save(user);
            log.info("Password reset for {}: {}", user.getEmail(), tempPassword);
            notificationService.sendTempPassword(user, tempPassword);
        });
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
