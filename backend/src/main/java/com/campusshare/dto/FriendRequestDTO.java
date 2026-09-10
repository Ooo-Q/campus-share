package com.campusshare.dto;

import lombok.Data;

@Data
public class FriendRequestDTO {
    private Long toUserId;
    private String message;
}
