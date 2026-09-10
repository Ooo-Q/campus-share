package com.campusshare.mapper;

import com.campusshare.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AnnouncementMapper {
    List<Announcement> findAll(@Param("keyword") String keyword);

    Announcement findById(@Param("id") Long id);

    int insert(Announcement announcement);

    int update(Announcement announcement);

    int delete(@Param("id") Long id);
}

