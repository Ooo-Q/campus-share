package com.campusshare.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FriendRequestVO {
    private Long id;
    private Long fromUserId;
    private String fromUsername;
    private String fromNickname;
    private String fromAvatar;
    private Long toUserId;
    private String toUsername;
    private String toNickname;
    private String toAvatar;
    private String message;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isFromMe;
}
