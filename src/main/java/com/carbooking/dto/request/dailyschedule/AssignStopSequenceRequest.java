package com.carbooking.dto.request.dailyschedule;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AssignStopSequenceRequest {
    @NotNull @Min(1) private Integer stopSequence;
}
