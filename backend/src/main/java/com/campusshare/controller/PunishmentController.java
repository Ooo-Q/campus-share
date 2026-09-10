package com.campusshare.controller;

import com.campusshare.entity.UserPunishment;
import com.campusshare.service.PunishmentService;
import com.campusshare.util.SecurityUtil;
import com.campusshare.vo.ApiResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/punishments")
@RequiredArgsConstructor
public class PunishmentController {

    private final PunishmentService punishmentService;

    @GetMapping("/my")
    public ApiResponse<List<UserPunishment>> listMyPunishments(@RequestParam(required = false) String status) {
        Long userId = SecurityUtil.requireLogin();
        if (status != null && !status.isEmpty()) {
            return ApiResponse.success(punishmentService.findByUserIdAndStatus(userId, status));
        }
        return ApiResponse.success(punishmentService.findByUserId(userId));
    }

    @PostMapping("/create")
    public ApiResponse<UserPunishment> createPunishment(@RequestBody CreatePunishmentRequest request) {
        Long adminId = SecurityUtil.requireAdmin();
        UserPunishment punishment = punishmentService.createDirect(
            adminId,
            request.getUserId(),
            request.getType(),
            request.getDuration(),
            request.getReason()
        );
        return ApiResponse.success(punishment);
    }

    @PostMapping("/create-for-resource")
    public ApiResponse<UserPunishment> createPunishmentForResource(@RequestBody CreateResourcePunishmentRequest request) {
        Long adminId = SecurityUtil.requireAdmin();
        UserPunishment punishment = punishmentService.createDirectForResource(
            adminId,
            request.getUserId(),
            request.getResourceId(),
            request.getResourceTitle(),
            request.getType(),
            request.getDuration(),
            request.getReason()
        );
        return ApiResponse.success(punishment);
    }

    @Data
    static class CreatePunishmentRequest {
        private Long userId;
        private String type;
        private Integer duration;
        private String reason;
    }

    @Data
    static class CreateResourcePunishmentRequest {
        private Long userId;
        private Long resourceId;
        private String resourceTitle;
        private String type;
        private Integer duration;
        private String reason;
    }
}
