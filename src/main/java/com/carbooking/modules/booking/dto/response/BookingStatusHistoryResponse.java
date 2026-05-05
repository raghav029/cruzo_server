package com.carbooking.modules.booking.dto.response;

import com.carbooking.common.enums.BookingStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class BookingStatusHistoryResponse {
    private UUID id;
    private BookingStatus fromStatus;
    private BookingStatus toStatus;
    private UUID actorUserId;
    private String reason;
    private Instant transitionedAt;
}
