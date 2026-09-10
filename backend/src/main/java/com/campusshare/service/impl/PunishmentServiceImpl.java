package com.campusshare.service.impl;

import com.campusshare.entity.ResourceReport;
import com.campusshare.entity.User;
import com.campusshare.entity.UserPunishment;
import com.campusshare.entity.UserReport;
import com.campusshare.mapper.ResourceMapper;
import com.campusshare.mapper.ResourceReportMapper;
import com.campusshare.mapper.UserMapper;
import com.campusshare.mapper.UserPunishmentMapper;
import com.campusshare.mapper.UserReportMapper;
import com.campusshare.service.PunishmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PunishmentServiceImpl implements PunishmentService {

    private final UserPunishmentMapper punishmentMapper;
    private final UserReportMapper userReportMapper;
    private final ResourceReportMapper resourceReportMapper;
    private final UserMapper userMapper;
    private final ResourceMapper resourceMapper;

    @Override
    public List<UserPunishment> findByUserId(Long userId) {
        return punishmentMapper.findByUserId(userId);
    }

    @Override
    public List<UserPunishment> findByUserIdAndStatus(Long userId, String status) {
        return punishmentMapper.findByUserIdAndStatus(userId, status);
    }

    @Override
    @Transactional
    public UserPunishment createFromReport(Long adminId, Long targetUserId, Long reportId, Long resourceId, String resourceTitle, String type, Integer duration, String reason) {
        UserPunishment punishment = new UserPunishment();
        punishment.setUserId(targetUserId);
        punishment.setReportId(reportId);
        punishment.setResourceId(resourceId);
        punishment.setResourceTitle(resourceTitle);
        punishment.setPunishmentType(type);
        punishment.setReason(reason);
        punishment.setDuration(duration);
        LocalDateTime now = LocalDateTime.now();
        punishment.setStartDate(now);
        if (duration != null && duration > 0) {
            punishment.setEndDate(now.plusDays(duration));
        }
        punishment.setStatus("ACTIVE");
        punishment.setAdminId(adminId);
        punishmentMapper.insert(punishment);
        return punishmentMapper.findById(punishment.getId());
    }

    @Override
    @Transactional
    public void removeByReportId(Long reportId) {
        punishmentMapper.deleteByReportId(reportId);
    }

    @Override
    public void checkUploadAllowed(Long userId) {
        if (userId == null) {
            return;
        }
        int cnt = punishmentMapper.countActiveByType(userId, "SUSPENSION", LocalDateTime.now());
        if (cnt > 0) {
            throw new com.campusshare.exception.BusinessException("您已被禁止上传资料，请在处罚结束后再试");
        }
    }

    @Override
    @Transactional
    public UserPunishment createFromUserReport(Long adminId, Long targetUserId, Long reportId, String reportedUsername, String type, Integer duration, String reason) {
        UserPunishment punishment = new UserPunishment();
        punishment.setUserId(targetUserId);
        punishment.setReportId(reportId);
        punishment.setReportedUserId(targetUserId);
        punishment.setReportedUsername(reportedUsername);
        punishment.setPunishmentType(type);
        punishment.setReason(reason);
        punishment.setDuration(duration);
        LocalDateTime now = LocalDateTime.now();
        punishment.setStartDate(now);
        if (duration != null && duration > 0) {
            punishment.setEndDate(now.plusDays(duration));
        }
        punishment.setStatus("ACTIVE");
        punishment.setAdminId(adminId);
        punishmentMapper.insert(punishment);
        return punishmentMapper.findById(punishment.getId());
    }

    @Override
    public boolean isMuted(Long userId) {
        if (userId == null) {
            return false;
        }
        int cnt = punishmentMapper.countActiveByType(userId, "MUTE", LocalDateTime.now());
        return cnt > 0;
    }

    @Override
    @Transactional
    public UserPunishment createDirect(Long adminId, Long targetUserId, String type, Integer duration, String reason) {
        UserPunishment punishment = new UserPunishment();
        punishment.setUserId(targetUserId);
        punishment.setPunishmentType(type);
        punishment.setReason(reason);
        punishment.setDuration(duration);
        LocalDateTime now = LocalDateTime.now();
        punishment.setStartDate(now);
        if (duration != null && duration > 0) {
            punishment.setEndDate(now.plusDays(duration));
        }
        punishment.setStatus("ACTIVE");
        punishment.setAdminId(adminId);
        punishmentMapper.insert(punishment);

        User targetUser = userMapper.findById(targetUserId);
        if (targetUser != null) {
            UserReport report = new UserReport();
            report.setReportedUserId(targetUserId);
            report.setUserId(adminId);
            report.setReason(reason != null && !reason.isEmpty() ? reason : "管理员直接处罚");
            report.setStatus("RESOLVED");
            report.setReviewReply("管理员直接处罚：" + reason);
            report.setCancelled(false);
            report.setReportedUsername(targetUser.getUsername());
            report.setReviewedAt(now);
            userReportMapper.insert(report);
            if (report.getId() != null) {
                punishment.setReportId(report.getId());
                punishmentMapper.update(punishment);
            }
        }

        return punishmentMapper.findById(punishment.getId());
    }

    @Override
    @Transactional
    public UserPunishment createDirectForResource(Long adminId, Long targetUserId, Long resourceId, String resourceTitle, String type, Integer duration, String reason) {
        UserPunishment punishment = new UserPunishment();
        punishment.setUserId(targetUserId);
        punishment.setResourceId(resourceId);
        punishment.setResourceTitle(resourceTitle);
        punishment.setPunishmentType(type);
        punishment.setReason(reason);
        punishment.setDuration(duration);
        LocalDateTime now = LocalDateTime.now();
        punishment.setStartDate(now);
        if (duration != null && duration > 0) {
            punishment.setEndDate(now.plusDays(duration));
        }
        punishment.setStatus("ACTIVE");
        punishment.setAdminId(adminId);
        punishmentMapper.insert(punishment);

        com.campusshare.entity.Resource resource = resourceMapper.findById(resourceId);
        if (resource != null) {
            ResourceReport report = new ResourceReport();
            report.setResourceId(resourceId);
            report.setUserId(adminId);
            report.setReason(reason != null && !reason.isEmpty() ? reason : "管理员直接处罚");
            report.setStatus("RESOLVED");
            report.setReviewReply("管理员直接处罚：" + reason);
            report.setCancelled(false);
            report.setResourceTitle(resourceTitle != null ? resourceTitle : resource.getTitle());
            report.setReviewedAt(now);
            resourceReportMapper.insert(report);
            Long reportId = report.getId();
            log.info("创建资料举报记录，reportId={}, resourceId={}, adminId={}, status={}",
                reportId, resourceId, adminId, report.getStatus());
            if (reportId != null) {
                punishment.setReportId(reportId);
                punishmentMapper.update(punishment);
                log.info("已关联处罚记录到举报，punishmentId={}, reportId={}", punishment.getId(), reportId);
            } else {
                log.warn("资料举报记录ID未自动填充，resourceId={}, adminId={}", resourceId, adminId);
            }
        }

        return punishmentMapper.findById(punishment.getId());
    }
}
