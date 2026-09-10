package com.campusshare.service;

import com.campusshare.dto.ResourceRequest;
import com.campusshare.entity.Resource;
import com.campusshare.vo.PageResponse;

public interface ResourceService {
    PageResponse<Resource> pageList(Integer page, Integer size, Long categoryId, String keyword, String visibility, Long ownerId);

    Resource getDetail(Long id, boolean increaseView);

    Resource create(Long userId, String username, ResourceRequest request);

    Resource update(Long id, Long userId, boolean admin, ResourceRequest request);

    void delete(Long id, Long userId, boolean admin);

    Resource updateVisibility(Long id, Long userId, boolean admin, String visibility);

    void recordDownload(Long id);
}
