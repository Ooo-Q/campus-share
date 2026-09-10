package com.campusshare.mapper;

import com.campusshare.entity.Resource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ResourceMapper {
    Resource findById(@Param("id") Long id);

    List<Resource> search(@Param("categoryId") Long categoryId,
                          @Param("keyword") String keyword,
                          @Param("visibility") String visibility,
                          @Param("ownerId") Long ownerId);

    int insert(Resource resource);

    int update(Resource resource);

    int delete(@Param("id") Long id);

    int updateVisibility(@Param("id") Long id, @Param("visibility") String visibility);

    int incrementDownload(@Param("id") Long id);

    int incrementView(@Param("id") Long id);

    int countByOwner(@Param("ownerId") Long ownerId);
}
