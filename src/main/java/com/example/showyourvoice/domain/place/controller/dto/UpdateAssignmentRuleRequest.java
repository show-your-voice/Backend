package com.example.showyourvoice.domain.place.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateAssignmentRuleRequest(@NotNull @Positive Long teacherId) {
}
