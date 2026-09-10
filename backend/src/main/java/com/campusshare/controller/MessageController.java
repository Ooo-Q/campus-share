package com.campusshare.controller;

import com.campusshare.dto.SendMessageRequest;
import com.campusshare.service.MessageService;
import com.campusshare.util.SecurityUtil;
import com.campusshare.vo.ApiResponse;
import com.campusshare.vo.ConversationVO;
import com.campusshare.vo.MessageVO;
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
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/conversations")
    public ApiResponse<List<ConversationVO>> getConversations() {
        Long userId = SecurityUtil.requireLogin();
        return ApiResponse.success(messageService.getConversations(userId));
    }

    @GetMapping("/conversations/{userId}")
    public ApiResponse<List<MessageVO>> getMessages(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Long currentUserId = SecurityUtil.requireLogin();
        return ApiResponse.success(messageService.getMessages(currentUserId, userId, page, size));
    }

    @PostMapping
    public ApiResponse<MessageVO> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        Long userId = SecurityUtil.requireLogin();
        return ApiResponse.success(messageService.sendMessage(userId, request));
    }

    @PutMapping("/conversations/{userId}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long userId) {
        Long currentUserId = SecurityUtil.requireLogin();
        messageService.markAsRead(currentUserId, userId);
        return ApiResponse.success("已标记为已读", null);
    }

    @GetMapping("/unread-count")
    public ApiResponse<Integer> getUnreadCount() {
        Long userId = SecurityUtil.requireLogin();
        return ApiResponse.success(messageService.getUnreadCount(userId));
    }

    @DeleteMapping("/conversations/{userId}")
    public ApiResponse<Void> deleteConversation(@PathVariable Long userId) {
        Long currentUserId = SecurityUtil.requireLogin();
        messageService.deleteConversation(currentUserId, userId);
        return ApiResponse.success("已删除对话记录", null);
    }
}

