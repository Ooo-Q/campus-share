package com.campusshare.service;

import com.campusshare.entity.UserPunishment;

import java.util.List;

public interface PunishmentService {
    List<UserPunishment> findByUserId(Long userId);

    List<UserPunishment> findByUserIdAndStatus(Long userId, String status);

    UserPunishment createFromReport(Long adminId, Long targetUserId, Long reportId, Long resourceId, String resourceTitle, String type, Integer duration, String reason);

    void removeByReportId(Long reportId);

    void checkUploadAllowed(Long userId);

    UserPunishment createFromUserReport(Long adminId, Long targetUserId, Long reportId, String reportedUsername, String type, Integer duration, String reason);

    boolean isMuted(Long userId);

    UserPunishment createDirect(Long adminId, Long targetUserId, String type, Integer duration, String reason);

    UserPunishment createDirectForResource(Long adminId, Long targetUserId, Long resourceId, String resourceTitle, String type, Integer duration, String reason);
}
