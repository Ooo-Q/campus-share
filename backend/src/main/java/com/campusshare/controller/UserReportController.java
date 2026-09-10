package com.campusshare.controller;

import com.campusshare.dto.ReportReviewRequest;
import com.campusshare.dto.UserReportRequest;
import com.campusshare.entity.UserReport;
import com.campusshare.security.UserContext;
import com.campusshare.service.UserReportService;
import com.campusshare.util.SecurityUtil;
import com.campusshare.vo.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/user-reports")
@RequiredArgsConstructor
public class UserReportController {

    private final UserReportService userReportService;

    @GetMapping
    public ApiResponse<List<UserReport>> list(@RequestParam(required = false) String status) {
        SecurityUtil.requireAdmin();
        return ApiResponse.success(userReportService.list(status));
    }

    @PostMapping
    public ApiResponse<UserReport> create(@Valid @RequestBody UserReportRequest request) {
        Long userId = SecurityUtil.requireLogin();
        return ApiResponse.success(userReportService.create(userId, request));
    }

    @PutMapping("/review")
    public ApiResponse<UserReport> review(@Valid @RequestBody ReportReviewRequest request) {
        SecurityUtil.requireAdmin();
        Long adminId = UserContext.get().getId();
        return ApiResponse.success(userReportService.review(adminId, request));
    }

    @GetMapping("/my")
    public ApiResponse<List<UserReport>> listMyReports(@RequestParam(required = false) String status) {
        Long userId = SecurityUtil.requireLogin();
        return ApiResponse.success(userReportService.listByUserId(userId, status));
    }

    @PutMapping("/reopen")
    public ApiResponse<UserReport> reopen(@RequestParam Long reportId) {
        Long adminId = SecurityUtil.requireAdmin();
        return ApiResponse.success(userReportService.reopen(adminId, reportId));
    }
}
