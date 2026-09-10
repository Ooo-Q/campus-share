package com.campusshare.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserPunishment {
    private Long id;

    private Long userId;

    private Long reportId;

    private Long resourceId;

    private String resourceTitle;

    private Long reportedUserId;

    private String reportedUsername;

    private String punishmentType;

    private String reason;

    private Integer duration;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private String status;

    private Long adminId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
