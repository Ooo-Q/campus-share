package com.campusshare.mapper;

import com.campusshare.entity.UserPunishment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserPunishmentMapper {
    int insert(UserPunishment punishment);

    int update(UserPunishment punishment);

    UserPunishment findById(@Param("id") Long id);

    List<UserPunishment> findByUserId(@Param("userId") Long userId);

    List<UserPunishment> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    int deleteByReportId(@Param("reportId") Long reportId);

    int deleteByResourceId(@Param("resourceId") Long resourceId);

    int countActiveByType(@Param("userId") Long userId, @Param("type") String type, @Param("now") java.time.LocalDateTime now);
}

