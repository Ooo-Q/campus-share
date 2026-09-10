package com.campusshare.mapper;

import com.campusshare.entity.ResourceFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ResourceFavoriteMapper {
    ResourceFavorite find(@Param("userId") Long userId, @Param("resourceId") Long resourceId);

    List<ResourceFavorite> findByUser(@Param("userId") Long userId);

    int insert(ResourceFavorite favorite);

    int delete(@Param("id") Long id);

    int deleteByUserAndResource(@Param("userId") Long userId, @Param("resourceId") Long resourceId);

    int deleteByResourceId(@Param("resourceId") Long resourceId);

    int countByResourceOwner(@Param("ownerId") Long ownerId);
}
