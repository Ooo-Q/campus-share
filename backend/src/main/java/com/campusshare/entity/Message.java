package com.campusshare.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Message {
    private Long id;
    private Long fromUserId;
    private Long toUserId;
    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private Boolean deletedByFromUser;
    private Boolean deletedByToUser;
}
