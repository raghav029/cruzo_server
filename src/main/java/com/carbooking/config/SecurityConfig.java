package com.carbooking.config;

import com.carbooking.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(request -> {
                var config = new org.springframework.web.cors.CorsConfiguration();
                config.setAllowedOriginPatterns(java.util.List.of("*"));
                config.setAllowedMethods(java.util.List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
                config.setAllowedHeaders(java.util.List.of("*"));
                config.setAllowCredentials(false);
                config.setMaxAge(3600L);
                return config;
            }))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Preflight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Public
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/forgot-password").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                // Super Admin
                .requestMatchers("/api/admin/**").hasRole("SUPER_ADMIN")

                // Dashboard
                .requestMatchers(HttpMethod.GET, "/api/dashboard/**").hasRole("FLEET_MANAGER")

                // Fleet Manager setup
                .requestMatchers(HttpMethod.POST,   "/api/vehicles/**").hasRole("FLEET_MANAGER")
                .requestMatchers(HttpMethod.PUT,    "/api/vehicles/**").hasRole("FLEET_MANAGER")
                .requestMatchers(HttpMethod.DELETE, "/api/vehicles/**").hasRole("FLEET_MANAGER")
                .requestMatchers(HttpMethod.GET,   "/api/drivers/me").hasRole("DRIVER")
                .requestMatchers(HttpMethod.GET,   "/api/drivers/me/stats").hasRole("DRIVER")
                .requestMatchers(HttpMethod.PATCH, "/api/drivers/me").hasRole("DRIVER")
                .requestMatchers(HttpMethod.PATCH, "/api/drivers/me/availability").hasRole("DRIVER")
                .requestMatchers("/api/drivers/**").hasRole("FLEET_MANAGER")
                .requestMatchers(HttpMethod.GET,    "/api/corporate-clients/my").hasRole("CORPORATE_ADMIN")
                .requestMatchers(HttpMethod.POST,   "/api/corporate-clients/*/employees").hasAnyRole("FLEET_MANAGER", "CORPORATE_ADMIN")
                .requestMatchers(HttpMethod.POST,   "/api/corporate-clients/**").hasRole("FLEET_MANAGER")
                .requestMatchers(HttpMethod.PUT,    "/api/corporate-clients/**").hasRole("FLEET_MANAGER")
                .requestMatchers(HttpMethod.DELETE, "/api/corporate-clients/**").hasRole("FLEET_MANAGER")
                .requestMatchers("/api/pricing-config/**").hasRole("FLEET_MANAGER")
                .requestMatchers("/api/cancellation-config/**").hasRole("FLEET_MANAGER")

                // Booking transitions
                .requestMatchers(HttpMethod.POST, "/api/bookings/*/approve").hasRole("CORPORATE_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/bookings/*/reject").hasRole("CORPORATE_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/bookings/*/assign-driver").hasRole("FLEET_MANAGER")
                .requestMatchers(HttpMethod.POST, "/api/bookings/*/auto-assign").hasRole("FLEET_MANAGER")
                .requestMatchers(HttpMethod.GET,   "/api/bookings/my-active").hasRole("EMPLOYEE")
                .requestMatchers(HttpMethod.GET,   "/api/bookings/my-trip").hasRole("DRIVER")
                .requestMatchers(HttpMethod.PATCH, "/api/bookings/*/location").hasRole("DRIVER")
                .requestMatchers(HttpMethod.POST,  "/api/bookings/*/status").hasRole("DRIVER")
                .requestMatchers(HttpMethod.POST, "/api/bookings").hasRole("EMPLOYEE")
                .requestMatchers(HttpMethod.GET, "/api/employees/me").hasAnyRole("EMPLOYEE", "CORPORATE_ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/employees/me").hasAnyRole("EMPLOYEE", "CORPORATE_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/employees/me/policy").hasAnyRole("EMPLOYEE", "CORPORATE_ADMIN")

                // Invoice management
                .requestMatchers(HttpMethod.POST, "/api/invoices/generate").hasRole("FLEET_MANAGER")
                .requestMatchers(HttpMethod.POST, "/api/invoices/*/mark-sent").hasRole("FLEET_MANAGER")
                .requestMatchers(HttpMethod.POST, "/api/invoices/*/mark-paid").hasRole("FLEET_MANAGER")

                // Reports
                .requestMatchers("/api/reports/fleet-summary").hasRole("FLEET_MANAGER")
                .requestMatchers("/api/reports/corporate-spend").hasRole("CORPORATE_ADMIN")

                // SOS Alerts
                .requestMatchers(HttpMethod.POST, "/api/sos").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/sos/*/resolve").hasRole("FLEET_MANAGER")
                .requestMatchers(HttpMethod.GET, "/api/sos/**").hasRole("FLEET_MANAGER")

                // Recurring Bookings
                .requestMatchers("/api/recurring-bookings/**").authenticated()

                // OTP verification
                .requestMatchers(HttpMethod.POST, "/api/bookings/*/refresh-boarding-otp").hasRole("EMPLOYEE")
                .requestMatchers(HttpMethod.POST, "/api/bookings/*/verify-boarding-otp").hasRole("DRIVER")
                .requestMatchers(HttpMethod.POST, "/api/bookings/*/verify-drop-otp").hasRole("DRIVER")

                // Document Expiry
                .requestMatchers("/api/documents/**").hasRole("FLEET_MANAGER")

                // Daily Schedules
                .requestMatchers(HttpMethod.POST, "/api/daily-schedules").hasRole("CORPORATE_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/daily-schedules/**").hasAnyRole("CORPORATE_ADMIN", "FLEET_MANAGER")
                .requestMatchers(HttpMethod.DELETE, "/api/daily-schedules/**").hasRole("CORPORATE_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/daily-schedules/**").hasAnyRole("CORPORATE_ADMIN", "FLEET_MANAGER")

                // Daily Trips
                .requestMatchers(HttpMethod.GET, "/api/daily-trips/my-today").hasRole("EMPLOYEE")
                .requestMatchers(HttpMethod.GET, "/api/daily-trips/my-today-driver").hasRole("DRIVER")
                .requestMatchers(HttpMethod.GET, "/api/daily-trips/my-schedule").hasRole("EMPLOYEE")
                .requestMatchers(HttpMethod.POST, "/api/daily-trips/enrollments/*/skip").hasRole("EMPLOYEE")
                .requestMatchers(HttpMethod.DELETE, "/api/daily-trips/enrollments/**").hasRole("EMPLOYEE")
                .requestMatchers(HttpMethod.GET, "/api/daily-trips/enrollments/**").hasRole("EMPLOYEE")
                .requestMatchers(HttpMethod.POST, "/api/daily-trips/*/assign-driver").hasRole("FLEET_MANAGER")
                .requestMatchers(HttpMethod.POST, "/api/daily-trips/*/cancel").hasRole("FLEET_MANAGER")
                .requestMatchers(HttpMethod.POST, "/api/daily-trips/*/complete").hasRole("DRIVER")
                .requestMatchers(HttpMethod.POST, "/api/daily-trips/*/passengers/*/board").hasRole("DRIVER")
                .requestMatchers(HttpMethod.POST, "/api/daily-trips/*/passengers/*/drop").hasRole("DRIVER")
                .requestMatchers(HttpMethod.POST, "/api/daily-trips/*/passengers/*/no-show").hasRole("DRIVER")
                .requestMatchers(HttpMethod.GET, "/api/daily-trips/**").hasAnyRole("FLEET_MANAGER", "CORPORATE_ADMIN")

                // All other authenticated
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
