package com.campusshare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportReviewRequest {
    @NotNull
    private Long reportId;

    @NotBlank
    private String status;

    private String reviewReply;

    private String action;

    private String punishmentType;

    private Integer punishmentDuration;

    private String punishmentReason;
}
