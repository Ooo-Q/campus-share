package com.campusshare.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FriendVO {
    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private String status;
    private LocalDateTime createdAt;
    private Boolean hasFriendRelation;
}
