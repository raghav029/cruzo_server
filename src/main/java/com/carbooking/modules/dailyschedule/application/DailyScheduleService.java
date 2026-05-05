package com.carbooking.modules.dailyschedule.application;

import com.carbooking.common.enums.Role;
import com.carbooking.common.exception.BusinessRuleException;
import com.carbooking.common.exception.DuplicateResourceException;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.exception.UnauthorizedException;
import com.carbooking.common.util.TenantSupport;
import com.carbooking.entity.*;
import com.carbooking.modules.dailyschedule.dto.request.*;
import com.carbooking.modules.dailyschedule.dto.response.DailySchedulePassengerResponse;
import com.carbooking.modules.dailyschedule.dto.response.DailyScheduleResponse;
import com.carbooking.modules.notification.application.NotificationService;
import com.carbooking.modules.client.domain.port.CorporateClientPort;
import com.carbooking.modules.dailyschedule.domain.port.DailySchedulePassengerPort;
import com.carbooking.modules.dailyschedule.domain.port.DailySchedulePort;
import lombok.extern.slf4j.Slf4j;
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
import com.carbooking.modules.dailyschedule.application.DailyScheduleMapper;

@Slf4j
@Service
public class DailyScheduleService extends TenantSupport {

    private final DailyScheduleMapper scheduleMapper;
    private final DailySchedulePort dailyScheduleRepository;
    private final DailySchedulePassengerPort dailySchedulePassengerRepository;
    private final CorporateClientPort corporateClientRepository;
    private final NotificationService notificationService;

    public DailyScheduleService(DailySchedulePort dailyScheduleRepository,
                                 DailySchedulePassengerPort dailySchedulePassengerRepository,
                                 CorporateClientPort corporateClientRepository,
                                 @Lazy NotificationService notificationService,
                                 DailyScheduleMapper scheduleMapper) {
        this.dailyScheduleRepository = dailyScheduleRepository;
        this.dailySchedulePassengerRepository = dailySchedulePassengerRepository;
        this.corporateClientRepository = corporateClientRepository;
        this.notificationService = notificationService;
        this.scheduleMapper = scheduleMapper;
    }

    @Transactional
    public DailyScheduleResponse create(CreateDailyScheduleRequest request) {
        Tenant tenant = requireTenant();
        CorporateClient corporateClient = corporateClientRepository.findById(request.getCorporateClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Corporate client not found"));
        assertSameTenant(corporateClient.getTenant().getId());

        User createdBy = currentUser();

        DailySchedule schedule = DailySchedule.builder()
                .tenant(tenant)
                .corporateClient(corporateClient)
                .name(request.getName())
                .vehicleType(request.getVehicleType())
                .recurrenceDays(String.join(",", request.getRecurrenceDays()))
                .pickupTime(request.getPickupTime())
                .dropAddress(request.getDropAddress())
                .dropLat(request.getDropLat())
                .dropLng(request.getDropLng())
                .isPooled(request.isPooled())
                .maxCapacity(request.getMaxCapacity())
                .isActive(true)
                .createdBy(createdBy)
                .build();

        schedule = dailyScheduleRepository.save(schedule);
        return scheduleMapper.toResponse(schedule, 0);
    }

    public Page<DailyScheduleResponse> list(Pageable pageable) {
        Tenant tenant = requireTenant();
        return dailyScheduleRepository.findByTenant(tenant, pageable)
                .map(s -> scheduleMapper.toResponse(s, dailySchedulePassengerRepository.findByDailyScheduleAndIsActiveTrue(s).size()));
    }

    public DailyScheduleResponse get(UUID id) {
        DailySchedule schedule = findSchedule(id);
        int count = dailySchedulePassengerRepository.findByDailyScheduleAndIsActiveTrue(schedule).size();
        return scheduleMapper.toResponse(schedule, count);
    }

    @Transactional
    public DailyScheduleResponse update(UUID id, UpdateDailyScheduleRequest request) {
        DailySchedule schedule = findSchedule(id);

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
        return scheduleMapper.toResponse(schedule, count);
    }

    @Transactional
    public void deactivate(UUID id) {
        DailySchedule schedule = findSchedule(id);
        schedule.setActive(false);
        dailyScheduleRepository.save(schedule);
    }

    @Transactional
    public DailySchedulePassengerResponse enrollPassenger(UUID scheduleId, EnrollPassengerRequest request) {
        Tenant tenant = requireTenant();
        DailySchedule schedule = findSchedule(scheduleId);

        User employee = userRepository.findByEmailAndTenant(request.getEmployeeEmail(), tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with email: " + request.getEmployeeEmail()));

        if (employee.getRole() != Role.EMPLOYEE) {
            throw new BusinessRuleException("User is not an employee");
        }

        if (dailySchedulePassengerRepository.existsByDailyScheduleAndEmployee(schedule, employee)) {
            throw new DuplicateResourceException("Employee is already enrolled in this schedule");
        }

        long activeCount = dailySchedulePassengerRepository.findByDailyScheduleAndIsActiveTrue(schedule).size();
        if (activeCount >= schedule.getMaxCapacity()) {
            throw new BusinessRuleException("Schedule is at maximum capacity (" + schedule.getMaxCapacity() + ")");
        }

        User enrolledBy = currentUser();

        DailySchedulePassenger passenger = DailySchedulePassenger.builder()
                .tenant(tenant)
                .dailySchedule(schedule)
                .employee(employee)
                .pickupAddress(request.getPickupAddress())
                .pickupLat(request.getPickupLat())
                .pickupLng(request.getPickupLng())
                .stopSequence(null)
                .isActive(true)
                .enrolledBy(enrolledBy)
                .enrolledAt(Instant.now())
                .build();

        return scheduleMapper.toPassengerResponse(dailySchedulePassengerRepository.save(passenger));
    }

    @Transactional
    public void removePassenger(UUID scheduleId, UUID passengerId) {
        DailySchedule schedule = findSchedule(scheduleId);

        DailySchedulePassenger passenger = dailySchedulePassengerRepository.findById(passengerId)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger enrollment not found"));
        if (!passenger.getDailySchedule().getId().equals(scheduleId)) {
            throw new UnauthorizedException("Access denied");
        }

        passenger.setActive(false);
        dailySchedulePassengerRepository.save(passenger);
    }

    public List<DailySchedulePassengerResponse> listPassengers(UUID scheduleId) {
        DailySchedule schedule = findSchedule(scheduleId);

        return dailySchedulePassengerRepository.findByDailyScheduleAndIsActiveTrue(schedule)
                .stream()
                .sorted((a, b) -> {
                    if (a.getStopSequence() == null && b.getStopSequence() == null) return 0;
                    if (a.getStopSequence() == null) return 1;
                    if (b.getStopSequence() == null) return -1;
                    return a.getStopSequence().compareTo(b.getStopSequence());
                })
                .map(scheduleMapper::toPassengerResponse)
                .collect(Collectors.toList());
    }

    public List<DailySchedulePassengerResponse> listUnsequencedPassengers(UUID scheduleId) {
        DailySchedule schedule = findSchedule(scheduleId);

        return dailySchedulePassengerRepository
                .findByDailyScheduleAndIsActiveTrueAndStopSequenceIsNull(schedule)
                .stream()
                .map(scheduleMapper::toPassengerResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DailySchedulePassengerResponse assignStopSequence(UUID scheduleId, UUID passengerId, AssignStopSequenceRequest request) {
        DailySchedule schedule = findSchedule(scheduleId);

        DailySchedulePassenger passenger = dailySchedulePassengerRepository.findById(passengerId)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger enrollment not found"));
        if (!passenger.getDailySchedule().getId().equals(scheduleId)) {
            throw new UnauthorizedException("Access denied");
        }

        boolean duplicateExists = dailySchedulePassengerRepository
                .findByDailyScheduleAndIsActiveTrue(schedule)
                .stream()
                .anyMatch(p -> !p.getId().equals(passengerId)
                        && p.getStopSequence() != null
                        && p.getStopSequence().equals(request.getStopSequence()));
        if (duplicateExists) {
            throw new BusinessRuleException("Stop sequence " + request.getStopSequence() + " is already assigned to another passenger");
        }

        User assignedBy = currentUser();

        passenger.setStopSequence(request.getStopSequence());
        passenger.setSequenceAssignedBy(assignedBy);
        passenger.setSequenceAssignedAt(Instant.now());

        return scheduleMapper.toPassengerResponse(dailySchedulePassengerRepository.save(passenger));
    }

    private DailySchedule findSchedule(UUID id) {
        DailySchedule schedule = dailyScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Daily schedule not found"));
        assertSameTenant(schedule.getTenant().getId());
        return schedule;
    }


}
