package com.carbooking.dto.request.invoice;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MarkPaidRequest {
    private String paymentMode;
    private String paymentReference;
}
