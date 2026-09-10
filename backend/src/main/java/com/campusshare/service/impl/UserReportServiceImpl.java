package com.campusshare.service.impl;

import com.campusshare.dto.ReportReviewRequest;
import com.campusshare.dto.UserReportRequest;
import com.campusshare.entity.User;
import com.campusshare.entity.UserReport;
import com.campusshare.exception.BusinessException;
import com.campusshare.mapper.UserMapper;
import com.campusshare.mapper.UserReportMapper;
import com.campusshare.service.PunishmentService;
import com.campusshare.service.UserReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserReportServiceImpl implements UserReportService {

    private final UserReportMapper reportMapper;
    private final UserMapper userMapper;
    private final PunishmentService punishmentService;

    @Override
    @Transactional
    public UserReport create(Long userId, UserReportRequest request) {
        if (userId.equals(request.getReportedUserId())) {
            throw new BusinessException("不能举报自己");
        }
        User reportedUser = userMapper.findById(request.getReportedUserId());
        if (reportedUser == null) {
            throw new BusinessException("被举报的用户不存在");
        }
        UserReport report = new UserReport();
        report.setReportedUserId(request.getReportedUserId());
        report.setUserId(userId);
        report.setReason(request.getReason());
        report.setStatus("PENDING");
        report.setCancelled(false);
        report.setReportedUsername(reportedUser.getUsername());
        reportMapper.insert(report);
        return report;
    }

    @Override
    public List<UserReport> list(String status) {
        return reportMapper.findAll(status);
    }

    @Override
    @Transactional
    public UserReport review(Long adminId, ReportReviewRequest request) {
        UserReport report = reportMapper.findById(request.getReportId());
        if (report == null) {
            throw new BusinessException("举报不存在");
        }
        if (!"PENDING".equals(report.getStatus())) {
            throw new BusinessException("举报已处理或已撤销");
        }
        User reportedUser = userMapper.findById(report.getReportedUserId());
        if (reportedUser == null) {
            throw new BusinessException("被举报的用户不存在或已删除");
        }

        String action = request.getAction() == null ? "NONE" : request.getAction().toUpperCase();
        boolean needPunish = "PUNISH".equals(action);

        if (needPunish) {
            if (request.getPunishmentType() == null || request.getPunishmentType().isEmpty()) {
                throw new BusinessException("处罚类型不能为空");
            }
            String punishmentType = request.getPunishmentType();
            if (!"WARNING".equals(punishmentType) && !"MUTE".equals(punishmentType)) {
                throw new BusinessException("用户举报的处罚类型只能是WARNING（警告）或MUTE（禁言）");
            }
            punishmentService.createFromUserReport(
                    adminId,
                    report.getReportedUserId(),
                    report.getId(),
                    reportedUser.getUsername(),
                    punishmentType,
                    request.getPunishmentDuration(),
                    request.getPunishmentReason() == null ? "举报处理处罚" : request.getPunishmentReason()
            );
        }

        report.setStatus(request.getStatus());
        String reply = request.getReviewReply();
        if (reply == null || reply.isBlank()) {
            reply = "RESOLVED".equalsIgnoreCase(request.getStatus()) ? "已处理" : "已驳回";
        }
        report.setReviewReply(reply);
        report.setReviewedAt(LocalDateTime.now());
        reportMapper.update(report);
        return reportMapper.findById(report.getId());
    }

    @Override
    public List<UserReport> listByUserId(Long userId, String status) {
        return reportMapper.findByUserId(userId, status);
    }

    @Override
    @Transactional
    public UserReport reopen(Long adminId, Long reportId) {
        UserReport report = reportMapper.findById(reportId);
        if (report == null) {
            throw new BusinessException("举报不存在");
        }
        if ("PENDING".equals(report.getStatus())) {
            throw new BusinessException("举报尚未处理，无需撤销");
        }
        punishmentService.removeByReportId(reportId);

        report.setStatus("PENDING");
        report.setCancelled(false);
        report.setReviewReply(null);
        report.setReviewedAt(null);
        reportMapper.update(report);
        return reportMapper.findById(report.getId());
    }
}
