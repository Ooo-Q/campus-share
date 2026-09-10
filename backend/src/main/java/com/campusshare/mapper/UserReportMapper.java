package com.campusshare.mapper;

import com.campusshare.entity.UserReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserReportMapper {
    int insert(UserReport report);

    int update(UserReport report);

    UserReport findById(@Param("id") Long id);

    List<UserReport> findAll(@Param("status") String status);

    List<UserReport> findByUserId(@Param("userId") Long userId, @Param("status") String status);
}
