package com.carbooking.modules.addon.application;

import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.util.TenantSupport;
import com.carbooking.entity.Addon;
import com.carbooking.entity.BookingAddon;
import com.carbooking.entity.Tenant;
import com.carbooking.modules.addon.domain.port.AddonPort;
import com.carbooking.modules.addon.domain.port.BookingAddonPort;
import com.carbooking.modules.addon.dto.request.AddBookingAddonsRequest;
import com.carbooking.modules.addon.dto.request.CreateAddonRequest;
import com.carbooking.modules.addon.dto.request.UpdateAddonRequest;
import com.carbooking.modules.addon.dto.response.AddonResponse;
import com.carbooking.modules.addon.dto.response.BookingAddonResponse;
import com.carbooking.modules.booking.domain.port.BookingPort;
import com.carbooking.entity.Booking;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddonService extends TenantSupport {

    private final AddonPort addonRepository;
    private final BookingAddonPort bookingAddonRepository;
    private final BookingPort bookingRepository;

    @Transactional
    public AddonResponse create(CreateAddonRequest request) {
        Tenant tenant = requireTenant();
        Addon addon = Addon.builder()
                .tenant(tenant)
                .name(request.getName())
                .price(request.getPrice())
                .active(true)
                .build();
        return toResponse(addonRepository.save(addon));
    }

    public Page<AddonResponse> list(Pageable pageable) {
        Tenant tenant = requireTenant();
        return addonRepository.findByTenant(tenant, pageable).map(this::toResponse);
    }

    @Transactional
    public AddonResponse update(UUID id, UpdateAddonRequest request) {
        Addon addon = findAddon(id);
        assertSameTenant(addon.getTenant().getId());
        if (request.getName() != null) addon.setName(request.getName());
        if (request.getPrice() != null) addon.setPrice(request.getPrice());
        if (request.getActive() != null) addon.setActive(request.getActive());
        return toResponse(addonRepository.save(addon));
    }

    @Transactional
    public void delete(UUID id) {
        Addon addon = findAddon(id);
        assertSameTenant(addon.getTenant().getId());
        addonRepository.deleteById(id);
    }

    @Transactional
    public List<BookingAddonResponse> addToBooking(UUID bookingId, AddBookingAddonsRequest request) {
        Tenant tenant = requireTenant();
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        assertSameTenant(booking.getTenant().getId());

        bookingAddonRepository.deleteByBookingId(bookingId);

        List<BookingAddon> saved = request.getAddons().stream().map(item -> {
            Addon addon = findAddon(item.getAddonId());
            assertSameTenant(addon.getTenant().getId());
            BookingAddon ba = BookingAddon.builder()
                    .bookingId(bookingId)
                    .addonId(item.getAddonId())
                    .quantity(item.getQuantity())
                    .priceSnapshot(addon.getPrice())
                    .build();
            return bookingAddonRepository.save(ba);
        }).collect(Collectors.toList());

        return saved.stream().map(ba -> toBookingAddonResponse(ba, findAddon(ba.getAddonId()))).collect(Collectors.toList());
    }

    public List<BookingAddonResponse> getBookingAddons(UUID bookingId) {
        return bookingAddonRepository.findByBookingId(bookingId).stream()
                .map(ba -> toBookingAddonResponse(ba, findAddon(ba.getAddonId())))
                .collect(Collectors.toList());
    }

    private Addon findAddon(UUID id) {
        return addonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Addon not found"));
    }

    private AddonResponse toResponse(Addon a) {
        return AddonResponse.builder()
                .id(a.getId())
                .name(a.getName())
                .price(a.getPrice())
                .active(a.isActive())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private BookingAddonResponse toBookingAddonResponse(BookingAddon ba, Addon addon) {
        return BookingAddonResponse.builder()
                .addonId(ba.getAddonId())
                .name(addon.getName())
                .quantity(ba.getQuantity())
                .priceSnapshot(ba.getPriceSnapshot())
                .total(ba.getPriceSnapshot().multiply(java.math.BigDecimal.valueOf(ba.getQuantity())))
                .build();
    }
}
