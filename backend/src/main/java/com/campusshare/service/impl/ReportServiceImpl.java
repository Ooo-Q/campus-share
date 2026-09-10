package com.campusshare.service.impl;

import com.campusshare.dto.ReportRequest;
import com.campusshare.dto.ReportReviewRequest;
import com.campusshare.dto.ReportUpdateRequest;
import com.campusshare.entity.Resource;
import com.campusshare.entity.ResourceReport;
import com.campusshare.exception.BusinessException;
import com.campusshare.mapper.ResourceMapper;
import com.campusshare.mapper.ResourceReportMapper;
import com.campusshare.service.PunishmentService;
import com.campusshare.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ResourceReportMapper reportMapper;
    private final ResourceMapper resourceMapper;
    private final PunishmentService punishmentService;

    @Override
    @Transactional
    public ResourceReport create(Long userId, ReportRequest request) {
        Resource resource = resourceMapper.findById(request.getResourceId());
        if (resource == null) {
            throw new BusinessException("资料不存在");
        }
        ResourceReport report = new ResourceReport();
        report.setResourceId(request.getResourceId());
        report.setUserId(userId);
        report.setReason(request.getReason());
        report.setStatus("PENDING");
        report.setCancelled(false);
        report.setResourceTitle(resource.getTitle());
        reportMapper.insert(report);
        return report;
    }

    @Override
    public List<ResourceReport> list(String status) {
        return reportMapper.findAll(status);
    }

    @Override
    @Transactional
    public ResourceReport review(Long adminId, ReportReviewRequest request) {
        ResourceReport report = reportMapper.findById(request.getReportId());
        if (report == null) {
            throw new BusinessException("举报不存在");
        }
        if (!"PENDING".equals(report.getStatus())) {
            throw new BusinessException("举报已处理或已撤销");
        }
        Resource resource = resourceMapper.findById(report.getResourceId());
        if (resource == null) {
            throw new BusinessException("被举报的资料不存在或已删除");
        }
        String action = request.getAction() == null ? "NONE" : request.getAction().toUpperCase();
        boolean hideOnly = "HIDE".equals(action);
        boolean hideAndPunish = "HIDE_PUNISH".equals(action);
        boolean doHide = hideOnly || hideAndPunish;
        if ("DELETE".equals(action)) {
            throw new BusinessException("删除举报处理功能已关闭");
        }

        if (doHide) {
            resourceMapper.updateVisibility(report.getResourceId(), "HIDDEN");
            if (hideOnly) {
                punishmentService.createFromReport(
                        adminId,
                        resource.getOwnerId(),
                        report.getId(),
                        resource.getId(),
                        resource.getTitle(),
                        "WARNING",
                        null,
                        request.getPunishmentReason() == null ? "违规内容已隐藏" : request.getPunishmentReason()
                );
            }
        }
        if (hideAndPunish) {
            if (request.getPunishmentType() == null || request.getPunishmentType().isEmpty()) {
                throw new BusinessException("处罚类型不能为空");
            }
            punishmentService.createFromReport(
                    adminId,
                    resource.getOwnerId(),
                    report.getId(),
                    resource.getId(),
                    resource.getTitle(),
                    request.getPunishmentType(),
                    request.getPunishmentDuration(),
                    request.getPunishmentReason() == null ? "举报处理处罚" : request.getPunishmentReason()
            );
        }
        report.setStatus(request.getStatus());
        String reply = request.getReviewReply();
        if (reply == null || reply.isBlank()) {
            reply = "RESOLVED".equalsIgnoreCase(request.getStatus()) ? "已处理" : "已驳回";
            if (doHide) {
                reply = "违规内容已隐藏";
            }
        }
        report.setReviewReply(reply);
        report.setReviewedAt(LocalDateTime.now());
        reportMapper.update(report);
        return reportMapper.findById(report.getId());
    }

    @Override
    public List<ResourceReport> listByUserId(Long userId, String status) {
        return reportMapper.findByUserId(userId, status);
    }

    @Override
    @Transactional
    public ResourceReport cancel(Long userId, Long reportId) {
        ResourceReport report = reportMapper.findById(reportId);
        if (report == null) {
            throw new BusinessException("举报不存在");
        }
        if (!report.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此举报");
        }
        if (Boolean.TRUE.equals(report.getCancelled())) {
            throw new BusinessException("举报已撤销");
        }
        if (!"PENDING".equals(report.getStatus())) {
            throw new BusinessException("只能撤销待处理的举报");
        }
        report.setCancelled(true);
        report.setStatus("CANCELLED");
        reportMapper.update(report);
        return report;
    }

    @Override
    @Transactional
    public ResourceReport update(Long userId, ReportUpdateRequest request) {
        ResourceReport report = reportMapper.findById(request.getReportId());
        if (report == null) {
            throw new BusinessException("举报不存在");
        }
        if (!report.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此举报");
        }
        if (Boolean.TRUE.equals(report.getCancelled())) {
            throw new BusinessException("已撤销的举报不能修改");
        }
        if (!"PENDING".equals(report.getStatus())) {
            throw new BusinessException("只能修改待处理的举报");
        }
        report.setReason(request.getReason());
        reportMapper.update(report);
        return report;
    }

    @Override
    @Transactional
    public ResourceReport reopen(Long adminId, Long reportId) {
        ResourceReport report = reportMapper.findById(reportId);
        if (report == null) {
            throw new BusinessException("举报不存在");
        }
        if ("PENDING".equals(report.getStatus())) {
            throw new BusinessException("举报尚未处理，无需撤销");
        }
        punishmentService.removeByReportId(reportId);

        Resource resource = resourceMapper.findById(report.getResourceId());
        if (resource != null && "HIDDEN".equalsIgnoreCase(resource.getVisibility())) {
            resourceMapper.updateVisibility(report.getResourceId(), "VISIBLE");
        }

        report.setStatus("PENDING");
        report.setCancelled(false);
        report.setReviewReply(null);
        report.setReviewedAt(null);
        reportMapper.update(report);
        return reportMapper.findById(report.getId());
    }
}

