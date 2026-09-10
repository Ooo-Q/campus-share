package com.campusshare.controller;

import com.campusshare.dto.FriendRequestDTO;
import com.campusshare.service.FriendService;
import com.campusshare.util.SecurityUtil;
import com.campusshare.vo.ApiResponse;
import com.campusshare.vo.FriendRequestVO;
import com.campusshare.vo.FriendVO;
import com.campusshare.vo.UserProfileVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @GetMapping
    public ApiResponse<List<FriendVO>> getFriendList() {
        Long userId = SecurityUtil.requireLogin();
        return ApiResponse.success(friendService.getFriendList(userId));
    }

    @GetMapping("/blocked")
    public ApiResponse<List<FriendVO>> getBlockedList() {
        Long userId = SecurityUtil.requireLogin();
        return ApiResponse.success(friendService.getBlockedList(userId));
    }

    @PostMapping("/requests")
    public ApiResponse<Void> sendFriendRequest(@Valid @RequestBody FriendRequestDTO request) {
        Long userId = SecurityUtil.requireLogin();
        friendService.sendFriendRequest(userId, request);
        return ApiResponse.success("好友申请已发送", null);
    }

    @GetMapping("/requests/all")
    public ApiResponse<List<FriendRequestVO>> getAllRequests() {
        Long userId = SecurityUtil.requireLogin();
        return ApiResponse.success(friendService.getAllRequests(userId));
    }

    @PutMapping("/requests/{id}/accept")
    public ApiResponse<Void> acceptFriendRequest(@PathVariable Long id) {
        Long userId = SecurityUtil.requireLogin();
        friendService.acceptFriendRequest(userId, id);
        return ApiResponse.success("已接受好友申请", null);
    }

    @PutMapping("/requests/{id}/reject")
    public ApiResponse<Void> rejectFriendRequest(@PathVariable Long id) {
        Long userId = SecurityUtil.requireLogin();
        friendService.rejectFriendRequest(userId, id);
        return ApiResponse.success("已拒绝好友申请", null);
    }

    @DeleteMapping("/requests/{id}")
    public ApiResponse<Void> deleteRequest(@PathVariable Long id) {
        Long userId = SecurityUtil.requireLogin();
        friendService.deleteRequest(userId, id);
        return ApiResponse.success("已删除申请记录", null);
    }

    @DeleteMapping("/{friendId}")
    public ApiResponse<Void> deleteFriend(@PathVariable Long friendId) {
        Long userId = SecurityUtil.requireLogin();
        friendService.deleteFriend(userId, friendId);
        return ApiResponse.success("已删除好友", null);
    }

    @PostMapping("/{friendId}/block")
    public ApiResponse<Void> blockFriend(@PathVariable Long friendId) {
        Long userId = SecurityUtil.requireLogin();
        friendService.blockFriend(userId, friendId);
        return ApiResponse.success("已拉黑", null);
    }

    @DeleteMapping("/{friendId}/block")
    public ApiResponse<Void> unblockFriend(@PathVariable Long friendId) {
        Long userId = SecurityUtil.requireLogin();
        friendService.unblockFriend(userId, friendId);
        return ApiResponse.success("已取消拉黑", null);
    }

    @GetMapping("/search")
    public ApiResponse<List<UserProfileVO>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        SecurityUtil.requireLogin();
        return ApiResponse.success(friendService.searchUsers(keyword, page, size));
    }
}

