package com.campusshare.mapper;

import com.campusshare.entity.ResourceComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ResourceCommentMapper {

    int insert(ResourceComment comment);

    ResourceComment findById(@Param("id") Long id);

    List<ResourceComment> listRoots(@Param("resourceId") Long resourceId);

    List<ResourceComment> listByRootIds(@Param("rootIds") List<Long> rootIds);

    int updateRootId(@Param("id") Long id, @Param("rootId") Long rootId);

    int softDelete(@Param("id") Long id);

    int deleteByResourceId(@Param("resourceId") Long resourceId);
}

