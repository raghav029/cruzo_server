package com.carbooking.modules.report.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class CorporateSpendResponse {
    private long totalBookings;
    private BigDecimal totalSpend;
    private List<EmployeeStat> topEmployees;
    private List<MonthlyBreakdown> monthlyBreakdown;

    @Getter
    @Builder
    public static class EmployeeStat {
        private String employeeName;
        private long trips;
        private BigDecimal totalSpend;
    }

    @Getter
    @Builder
    public static class MonthlyBreakdown {
        private String month;
        private long trips;
        private BigDecimal spend;
    }
}
