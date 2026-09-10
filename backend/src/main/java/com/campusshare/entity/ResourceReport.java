package com.campusshare.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResourceReport {
    private Long id;

    private Long resourceId;

    private Long userId;

    private String reason;

    private String status;

    private String reviewReply;

    private Boolean cancelled;

    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;

    private LocalDateTime updatedAt;

    private String resourceVisibility;

    private Long resourceOwnerId;

    private String resourceTitle;
}
