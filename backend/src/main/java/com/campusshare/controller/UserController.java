package com.campusshare.controller;

import com.campusshare.entity.Resource;
import com.campusshare.security.UserContext;
import com.campusshare.service.FriendService;
import com.campusshare.service.ResourceService;
import com.campusshare.vo.ApiResponse;
import com.campusshare.vo.PageResponse;
import com.campusshare.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final FriendService friendService;
    private final ResourceService resourceService;

    @GetMapping("/{userId}/profile")
    public ApiResponse<UserProfileVO> getUserProfile(@PathVariable Long userId) {
        Long currentUserId = com.campusshare.security.UserContext.getUserId();
        if (currentUserId == null) {
            currentUserId = 0L;
        }
        return ApiResponse.success(friendService.getUserProfile(currentUserId, userId));
    }

    @GetMapping("/{userId}/resources")
    public ApiResponse<PageResponse<Resource>> getUserResources(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long currentUserId = UserContext.getUserId();
        boolean isAdmin = currentUserId != null && "ADMIN".equalsIgnoreCase(UserContext.get().getRole());
        boolean isSelf = currentUserId != null && currentUserId.equals(userId);

        String visibility = (isAdmin || isSelf) ? null : "VISIBLE";

        PageResponse<Resource> data = resourceService.pageList(page, size, null, null, visibility, userId);
        return ApiResponse.success(data);
    }
}
