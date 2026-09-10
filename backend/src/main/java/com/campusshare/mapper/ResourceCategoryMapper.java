package com.campusshare.mapper;

import com.campusshare.entity.ResourceCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ResourceCategoryMapper {
    List<ResourceCategory> findAll();

    ResourceCategory findById(@Param("id") Long id);

    int insert(ResourceCategory category);

    int update(ResourceCategory category);

    int delete(@Param("id") Long id);
}

