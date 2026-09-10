package com.campusshare.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FriendRequest {
    private Long id;
    private Long fromUserId;
    private Long toUserId;
    private String message;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
