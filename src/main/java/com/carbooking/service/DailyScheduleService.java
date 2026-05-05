package com.carbooking.service;

import com.carbooking.common.enums.Role;
import com.carbooking.common.exception.BusinessRuleException;
import com.carbooking.common.exception.DuplicateResourceException;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.exception.UnauthorizedException;
import com.carbooking.common.util.SecurityUtils;
import com.carbooking.dto.request.dailyschedule.*;
import com.carbooking.dto.response.dailyschedule.DailySchedulePassengerResponse;
import com.carbooking.dto.response.dailyschedule.DailyScheduleResponse;
import com.carbooking.entity.*;
import com.carbooking.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DailyScheduleService {

    private final DailyScheduleRepository dailyScheduleRepository;
    private final DailySchedulePassengerRepository dailySchedulePassengerRepository;
    private final TenantRepository tenantRepository;
    private final CorporateClientRepository corporateClientRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Autowired
    public DailyScheduleService(
            DailyScheduleRepository dailyScheduleRepository,
            DailySchedulePassengerRepository dailySchedulePassengerRepository,
            TenantRepository tenantRepository,
            CorporateClientRepository corporateClientRepository,
            UserRepository userRepository,
            @Lazy NotificationService notificationService) {
        this.dailyScheduleRepository = dailyScheduleRepository;
        this.dailySchedulePassengerRepository = dailySchedulePassengerRepository;
        this.tenantRepository = tenantRepository;
        this.corporateClientRepository = corporateClientRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public DailyScheduleResponse create(CreateDailyScheduleRequest request) {
        String role = SecurityUtils.getCurrentRole();
        if (!"ROLE_CORPORATE_ADMIN".equals(role)) {
            throw new UnauthorizedException("Only CORPORATE_ADMIN can create schedules");
        }

        UUID tenantId = SecurityUtils.getCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        CorporateClient corporateClient = corporateClientRepository.findById(request.getCorporateClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Corporate client not found"));
        if (!corporateClient.getTenant().getId().equals(tenantId)) {
            throw new UnauthorizedException("Access denied");
        }

        UUID userId = SecurityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String recurrenceDays = String.join(",", request.getRecurrenceDays());

        DailySchedule schedule = DailySchedule.builder()
                .tenant(tenant)
                .corporateClient(corporateClient)
                .name(request.getName())
                .vehicleType(request.getVehicleType())
                .recurrenceDays(recurrenceDays)
                .pickupTime(request.getPickupTime())
                .dropAddress(request.getDropAddress())
                .dropLat(request.getDropLat())
                .dropLng(request.getDropLng())
                .isPooled(request.isPooled())
                .maxCapacity(request.getMaxCapacity())
                .isActive(true)
                .createdBy(currentUser)
                .build();

        schedule = dailyScheduleRepository.save(schedule);
        return toResponse(schedule, 0);
    }

    public Page<DailyScheduleResponse> list(Pageable pageable) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        String role = SecurityUtils.getCurrentRole();

        if ("ROLE_FLEET_MANAGER".equals(role)) {
            return dailyScheduleRepository.findByTenant(tenant, pageable)
                    .map(s -> toResponse(s, dailySchedulePassengerRepository.findByDailyScheduleAndIsActiveTrue(s).size()));
        } else if ("ROLE_CORPORATE_ADMIN".equals(role)) {
            UUID userId = SecurityUtils.getCurrentUserId();
            User currentUser = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            // Find the corporate client this admin belongs to via their employee link
            // For CORPORATE_ADMIN we list all schedules for the tenant (they may manage multiple clients)
            return dailyScheduleRepository.findByTenant(tenant, pageable)
                    .map(s -> toResponse(s, dailySchedulePassengerRepository.findByDailyScheduleAndIsActiveTrue(s).size()));
        }

        throw new UnauthorizedException("Access denied");
    }

    public DailyScheduleResponse get(UUID id) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        DailySchedule schedule = dailyScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Daily schedule not found"));
        if (!schedule.getTenant().getId().equals(tenantId)) {
            throw new UnauthorizedException("Access denied");
        }
        int count = dailySchedulePassengerRepository.findByDailyScheduleAndIsActiveTrue(schedule).size();
        return toResponse(schedule, count);
    }

    @Transactional
    public DailyScheduleResponse update(UUID id, UpdateDailyScheduleRequest request) {
        String role = SecurityUtils.getCurrentRole();
        if (!"ROLE_CORPORATE_ADMIN".equals(role) && !"ROLE_FLEET_MANAGER".equals(role)) {
            throw new UnauthorizedException("Access denied");
        }

        UUID tenantId = SecurityUtils.getCurrentTenantId();
        DailySchedule schedule = dailyScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Daily schedule not found"));
        if (!schedule.getTenant().getId().equals(tenantId)) {
            throw new UnauthorizedException("Access denied");
        }

        if (request.getName() != null) schedule.setName(request.getName());
        if (request.getVehicleType() != null) schedule.setVehicleType(request.getVehicleType());
        if (request.getRecurrenceDays() != null) schedule.setRecurrenceDays(String.join(",", request.getRecurrenceDays()));
        if (request.getPickupTime() != null) schedule.setPickupTime(request.getPickupTime());
        if (request.getDropAddress() != null) schedule.setDropAddress(request.getDropAddress());
        if (request.getDropLat() != null) schedule.setDropLat(request.getDropLat());
        if (request.getDropLng() != null) schedule.setDropLng(request.getDropLng());
        if (request.getIsPooled() != null) schedule.setPooled(request.getIsPooled());
        if (request.getMaxCapacity() != null) schedule.setMaxCapacity(request.getMaxCapacity());

        schedule = dailyScheduleRepository.save(schedule);
        int count = dailySchedulePassengerRepository.findByDailyScheduleAndIsActiveTrue(schedule).size();
        return toResponse(schedule, count);
    }

    @Transactional
    public void deactivate(UUID id) {
        String role = SecurityUtils.getCurrentRole();
        if (!"ROLE_CORPORATE_ADMIN".equals(role)) {
            throw new UnauthorizedException("Only CORPORATE_ADMIN can deactivate schedules");
        }

        UUID tenantId = SecurityUtils.getCurrentTenantId();
        DailySchedule schedule = dailyScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Daily schedule not found"));
        if (!schedule.getTenant().getId().equals(tenantId)) {
            throw new UnauthorizedException("Access denied");
        }

        schedule.setActive(false);
        dailyScheduleRepository.save(schedule);
    }

    @Transactional
    public DailySchedulePassengerResponse enrollPassenger(UUID scheduleId, EnrollPassengerRequest request) {
        String role = SecurityUtils.getCurrentRole();
        if (!"ROLE_CORPORATE_ADMIN".equals(role)) {
            throw new UnauthorizedException("Only CORPORATE_ADMIN can enroll passengers");
        }

        UUID tenantId = SecurityUtils.getCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        DailySchedule schedule = dailyScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Daily schedule not found"));
        if (!schedule.getTenant().getId().equals(tenantId)) {
            throw new UnauthorizedException("Access denied");
        }

        User employee = userRepository.findByEmailAndTenant(request.getEmployeeEmail(), tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with email: " + request.getEmployeeEmail()));

        if (employee.getRole() != Role.EMPLOYEE) {
            throw new BusinessRuleException("User is not an employee");
        }

        if (dailySchedulePassengerRepository.existsByDailyScheduleAndEmployee(schedule, employee)) {
            throw new DuplicateResourceException("Employee is already enrolled in this schedule");
        }

        // Check capacity
        long activeCount = dailySchedulePassengerRepository.findByDailyScheduleAndIsActiveTrue(schedule).size();
        if (activeCount >= schedule.getMaxCapacity()) {
            throw new BusinessRuleException("Schedule is at maximum capacity (" + schedule.getMaxCapacity() + ")");
        }

        UUID userId = SecurityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        DailySchedulePassenger passenger = DailySchedulePassenger.builder()
                .tenant(tenant)
                .dailySchedule(schedule)
                .employee(employee)
                .pickupAddress(request.getPickupAddress())
                .pickupLat(request.getPickupLat())
                .pickupLng(request.getPickupLng())
                .stopSequence(null)
                .isActive(true)
                .enrolledBy(currentUser)
                .enrolledAt(Instant.now())
                .build();

        passenger = dailySchedulePassengerRepository.save(passenger);
        return toPassengerResponse(passenger);
    }

    @Transactional
    public void removePassenger(UUID scheduleId, UUID passengerId) {
        String role = SecurityUtils.getCurrentRole();
        if (!"ROLE_CORPORATE_ADMIN".equals(role)) {
            throw new UnauthorizedException("Only CORPORATE_ADMIN can remove passengers");
        }

        UUID tenantId = SecurityUtils.getCurrentTenantId();
        DailySchedule schedule = dailyScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Daily schedule not found"));
        if (!schedule.getTenant().getId().equals(tenantId)) {
            throw new UnauthorizedException("Access denied");
        }

        DailySchedulePassenger passenger = dailySchedulePassengerRepository.findById(passengerId)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger enrollment not found"));
        if (!passenger.getDailySchedule().getId().equals(scheduleId)) {
            throw new UnauthorizedException("Access denied");
        }

        passenger.setActive(false);
        dailySchedulePassengerRepository.save(passenger);
    }

    public List<DailySchedulePassengerResponse> listPassengers(UUID scheduleId) {
        String role = SecurityUtils.getCurrentRole();
        if (!"ROLE_FLEET_MANAGER".equals(role) && !"ROLE_CORPORATE_ADMIN".equals(role)) {
            throw new UnauthorizedException("Access denied");
        }

        UUID tenantId = SecurityUtils.getCurrentTenantId();
        DailySchedule schedule = dailyScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Daily schedule not found"));
        if (!schedule.getTenant().getId().equals(tenantId)) {
            throw new UnauthorizedException("Access denied");
        }

        return dailySchedulePassengerRepository.findByDailyScheduleAndIsActiveTrue(schedule)
                .stream()
                .sorted((a, b) -> {
                    if (a.getStopSequence() == null && b.getStopSequence() == null) return 0;
                    if (a.getStopSequence() == null) return 1;
                    if (b.getStopSequence() == null) return -1;
                    return a.getStopSequence().compareTo(b.getStopSequence());
                })
                .map(this::toPassengerResponse)
                .collect(Collectors.toList());
    }

    public List<DailySchedulePassengerResponse> listUnsequencedPassengers(UUID scheduleId) {
        String role = SecurityUtils.getCurrentRole();
        if (!"ROLE_FLEET_MANAGER".equals(role)) {
            throw new UnauthorizedException("Only FLEET_MANAGER can view unsequenced passengers");
        }

        UUID tenantId = SecurityUtils.getCurrentTenantId();
        DailySchedule schedule = dailyScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Daily schedule not found"));
        if (!schedule.getTenant().getId().equals(tenantId)) {
            throw new UnauthorizedException("Access denied");
        }

        return dailySchedulePassengerRepository
                .findByDailyScheduleAndIsActiveTrueAndStopSequenceIsNull(schedule)
                .stream()
                .map(this::toPassengerResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DailySchedulePassengerResponse assignStopSequence(UUID scheduleId, UUID passengerId, AssignStopSequenceRequest request) {
        String role = SecurityUtils.getCurrentRole();
        if (!"ROLE_FLEET_MANAGER".equals(role)) {
            throw new UnauthorizedException("Only FLEET_MANAGER can assign stop sequences");
        }

        UUID tenantId = SecurityUtils.getCurrentTenantId();
        DailySchedule schedule = dailyScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Daily schedule not found"));
        if (!schedule.getTenant().getId().equals(tenantId)) {
            throw new UnauthorizedException("Access denied");
        }

        DailySchedulePassenger passenger = dailySchedulePassengerRepository.findById(passengerId)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger enrollment not found"));
        if (!passenger.getDailySchedule().getId().equals(scheduleId)) {
            throw new UnauthorizedException("Access denied");
        }

        // Check no duplicate stop sequence on this schedule
        boolean duplicateExists = dailySchedulePassengerRepository
                .findByDailyScheduleAndIsActiveTrue(schedule)
                .stream()
                .anyMatch(p -> !p.getId().equals(passengerId)
                        && p.getStopSequence() != null
                        && p.getStopSequence().equals(request.getStopSequence()));
        if (duplicateExists) {
            throw new BusinessRuleException("Stop sequence " + request.getStopSequence() + " is already assigned to another passenger");
        }

        UUID userId = SecurityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        passenger.setStopSequence(request.getStopSequence());
        passenger.setSequenceAssignedBy(currentUser);
        passenger.setSequenceAssignedAt(Instant.now());
        passenger = dailySchedulePassengerRepository.save(passenger);

        return toPassengerResponse(passenger);
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private DailyScheduleResponse toResponse(DailySchedule schedule, int enrolledCount) {
        List<String> days = Arrays.asList(schedule.getRecurrenceDays().split(","));
        return DailyScheduleResponse.builder()
                .id(schedule.getId())
                .tenantId(schedule.getTenant().getId())
                .corporateClientId(schedule.getCorporateClient().getId())
                .corporateClientName(schedule.getCorporateClient().getCompanyName())
                .name(schedule.getName())
                .vehicleType(schedule.getVehicleType())
                .recurrenceDays(days)
                .pickupTime(schedule.getPickupTime())
                .dropAddress(schedule.getDropAddress())
                .isPooled(schedule.isPooled())
                .maxCapacity(schedule.getMaxCapacity())
                .isActive(schedule.isActive())
                .enrolledPassengerCount(enrolledCount)
                .createdAt(schedule.getCreatedAt())
                .build();
    }

    public DailySchedulePassengerResponse toPassengerResponse(DailySchedulePassenger p) {
        return DailySchedulePassengerResponse.builder()
                .id(p.getId())
                .employeeUserId(p.getEmployee().getId())
                .employeeName(p.getEmployee().getFullName())
                .employeeEmail(p.getEmployee().getEmail())
                .employeePhone(p.getEmployee().getPhone())
                .pickupAddress(p.getPickupAddress())
                .pickupLat(p.getPickupLat())
                .pickupLng(p.getPickupLng())
                .stopSequence(p.getStopSequence())
                .isActive(p.isActive())
                .enrolledAt(p.getEnrolledAt())
                .sequenceAssignedAt(p.getSequenceAssignedAt())
                .build();
    }
}
