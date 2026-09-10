package com.campusshare.service;

import com.campusshare.dto.ReportRequest;
import com.campusshare.dto.ReportReviewRequest;
import com.campusshare.dto.ReportUpdateRequest;
import com.campusshare.entity.ResourceReport;

import java.util.List;

public interface ReportService {
    ResourceReport create(Long userId, ReportRequest request);

    List<ResourceReport> list(String status);

    ResourceReport review(Long adminId, ReportReviewRequest request);

    List<ResourceReport> listByUserId(Long userId, String status);

    ResourceReport cancel(Long userId, Long reportId);

    ResourceReport update(Long userId, ReportUpdateRequest request);

    ResourceReport reopen(Long adminId, Long reportId);
}
