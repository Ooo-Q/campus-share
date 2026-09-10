package com.campusshare.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserProfileVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String gender;
    private String email;
    private String phone;
    private LocalDateTime createdAt;
    private Integer resourceCount;
    private Integer favoriteCount;
    private Integer likeCount;
    private String friendStatus;
    private Long friendRequestId;
}
