package com.campusshare.service;

import com.campusshare.dto.ReportReviewRequest;
import com.campusshare.dto.UserReportRequest;
import com.campusshare.entity.UserReport;

import java.util.List;

public interface UserReportService {
    UserReport create(Long userId, UserReportRequest request);

    List<UserReport> list(String status);

    UserReport review(Long adminId, ReportReviewRequest request);

    List<UserReport> listByUserId(Long userId, String status);

    UserReport reopen(Long adminId, Long reportId);
}
