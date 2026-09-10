package com.campusshare.mapper;

import com.campusshare.entity.ResourceLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ResourceLikeMapper {
    ResourceLike find(@Param("userId") Long userId, @Param("resourceId") Long resourceId);

    List<ResourceLike> findByUser(@Param("userId") Long userId);

    int insert(ResourceLike like);

    int delete(@Param("id") Long id);

    int deleteByUserAndResource(@Param("userId") Long userId, @Param("resourceId") Long resourceId);

    int deleteByResourceId(@Param("resourceId") Long resourceId);

    int countByResourceOwner(@Param("ownerId") Long ownerId);
}
