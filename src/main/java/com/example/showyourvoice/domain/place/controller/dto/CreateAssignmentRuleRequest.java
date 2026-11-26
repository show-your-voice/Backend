package com.example.showyourvoice.domain.place.controller.dto;

import com.example.showyourvoice.domain.post.domain.PostCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateAssignmentRuleRequest(@NotNull @Positive Long roomId, @NotNull PostCategory category,
                                          @NotNull @Positive Long teacherId) {
}
