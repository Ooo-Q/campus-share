package com.campusshare.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private Long userId;

    private String username;

    private String nickname;

    private String role;

    private String token;

    private String avatar;
}
