package com.campusshare.mapper;

import com.campusshare.entity.Resource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminMapper {

    long countPendingReports();

    List<Resource> latestResources(@Param("limit") int limit);
}


