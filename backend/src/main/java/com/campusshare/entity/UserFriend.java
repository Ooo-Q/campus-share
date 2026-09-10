package com.campusshare.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserFriend {
    private Long id;
    private Long userId;
    private Long friendId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
