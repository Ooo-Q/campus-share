package com.campusshare.controller;

import com.campusshare.dto.ReportRequest;
import com.campusshare.dto.ReportReviewRequest;
import com.campusshare.dto.ReportUpdateRequest;
import com.campusshare.entity.ResourceReport;
import com.campusshare.security.UserContext;
import com.campusshare.service.ReportService;
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
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    public ApiResponse<List<ResourceReport>> list(@RequestParam(required = false) String status) {
        SecurityUtil.requireAdmin();
        return ApiResponse.success(reportService.list(status));
    }

    @PostMapping
    public ApiResponse<ResourceReport> create(@Valid @RequestBody ReportRequest request) {
        Long userId = SecurityUtil.requireLogin();
        return ApiResponse.success(reportService.create(userId, request));
    }

    @PutMapping("/review")
    public ApiResponse<ResourceReport> review(@Valid @RequestBody ReportReviewRequest request) {
        SecurityUtil.requireAdmin();
        Long adminId = UserContext.get().getId();
        return ApiResponse.success(reportService.review(adminId, request));
    }

    @GetMapping("/my")
    public ApiResponse<List<ResourceReport>> listMyReports(@RequestParam(required = false) String status) {
        Long userId = SecurityUtil.requireLogin();
        return ApiResponse.success(reportService.listByUserId(userId, status));
    }

    @PutMapping("/cancel")
    public ApiResponse<ResourceReport> cancel(@RequestParam Long reportId) {
        Long userId = SecurityUtil.requireLogin();
        return ApiResponse.success(reportService.cancel(userId, reportId));
    }

    @PutMapping("/update")
    public ApiResponse<ResourceReport> update(@Valid @RequestBody ReportUpdateRequest request) {
        Long userId = SecurityUtil.requireLogin();
        return ApiResponse.success(reportService.update(userId, request));
    }

    @PutMapping("/reopen")
    public ApiResponse<ResourceReport> reopen(@RequestParam Long reportId) {
        SecurityUtil.requireAdmin();
        Long adminId = UserContext.get().getId();
        return ApiResponse.success(reportService.reopen(adminId, reportId));
    }
}
