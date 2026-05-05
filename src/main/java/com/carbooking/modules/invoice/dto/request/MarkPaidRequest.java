package com.carbooking.modules.invoice.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MarkPaidRequest {
    private String paymentMode;
    private String paymentReference;
}
