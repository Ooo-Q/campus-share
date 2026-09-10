package com.campusshare.mapper;

import com.campusshare.entity.ResourceReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ResourceReportMapper {
    int insert(ResourceReport report);

    int update(ResourceReport report);

    ResourceReport findById(@Param("id") Long id);

    List<ResourceReport> findAll(@Param("status") String status);

    List<ResourceReport> findByUserId(@Param("userId") Long userId, @Param("status") String status);

    int deleteByResourceId(@Param("resourceId") Long resourceId);

    List<ResourceReport> findByResourceId(@Param("resourceId") Long resourceId);
}
