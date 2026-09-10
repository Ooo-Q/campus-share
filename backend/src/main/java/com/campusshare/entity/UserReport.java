package com.campusshare.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserReport {
    private Long id;

    private Long reportedUserId;

    private Long userId;

    private String reason;

    private String status;

    private String reviewReply;

    private String reportedUsername;

    private Boolean cancelled;

    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;

    private LocalDateTime updatedAt;
}
