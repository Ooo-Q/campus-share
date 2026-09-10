package com.campusshare.util;

import com.campusshare.exception.BusinessException;
import com.campusshare.security.UserContext;

public final class SecurityUtil {

    private SecurityUtil() {}

    public static Long requireLogin() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("请先登录");
        }
        return userId;
    }

    public static Long requireAdmin() {
        Long userId = requireLogin();
        if (!"ADMIN".equalsIgnoreCase(UserContext.getRole())) {
            throw new BusinessException("无权访问");
        }
        return userId;
    }
}
