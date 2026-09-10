package com.campusshare.service.impl;

import com.campusshare.dto.ResourceRequest;
import com.campusshare.entity.Resource;
import com.campusshare.entity.ResourceReport;
import com.campusshare.exception.BusinessException;
import com.campusshare.mapper.ResourceCategoryMapper;
import com.campusshare.mapper.ResourceCommentMapper;
import com.campusshare.mapper.ResourceFavoriteMapper;
import com.campusshare.mapper.ResourceLikeMapper;
import com.campusshare.mapper.ResourceMapper;
import com.campusshare.mapper.ResourceReportMapper;
import com.campusshare.mapper.UserPunishmentMapper;
import com.campusshare.service.FileStorageService;
import com.campusshare.service.ResourceService;
import com.campusshare.vo.PageResponse;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final ResourceMapper resourceMapper;
    private final ResourceCategoryMapper categoryMapper;
    private final ResourceFavoriteMapper favoriteMapper;
    private final ResourceLikeMapper likeMapper;
    private final ResourceReportMapper reportMapper;
    private final ResourceCommentMapper commentMapper;
    private final UserPunishmentMapper punishmentMapper;
    private final FileStorageService fileStorageService;

    @Override
    @Cacheable(
            cacheNames = "resourceList",
            key = "#page + ':' + #size + ':' + (#categoryId != null ? #categoryId : 'all') + ':' + (#keyword != null ? #keyword : '')",
            condition = "#visibility == 'VISIBLE' and #ownerId == null"
    )
    public PageResponse<Resource> pageList(Integer page, Integer size, Long categoryId, String keyword, String visibility, Long ownerId) {
        int pageNum = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : size;

        log.info("查询资料列表（可能打到 MySQL）page={}, size={}, categoryId={}, keyword={}", pageNum, pageSize, categoryId, keyword);

        PageHelper.startPage(pageNum, pageSize);

        List<Resource> list = resourceMapper.search(categoryId, keyword, visibility, ownerId);

        PageInfo<Resource> info = new PageInfo<>(list);

        return PageResponse.of(info.getTotal(), pageNum, pageSize, list);
    }

    @Override
    public Resource getDetail(Long id, boolean increaseView) {
        Resource resource = resourceMapper.findById(id);
        if (resource == null) {
            throw new BusinessException("资料不存在");
        }

        if (increaseView) {
            resourceMapper.incrementView(id);
            resource = resourceMapper.findById(id);
        }
        return resource;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "resourceList", allEntries = true)
    public Resource create(Long userId, String username, ResourceRequest request) {
        if (categoryMapper.findById(request.getCategoryId()) == null) {
            throw new BusinessException("分类不存在");
        }

        String safeFileUrl = request.getFileUrl() == null ? "" : request.getFileUrl();

        Resource resource = new Resource();
        resource.setTitle(request.getTitle());
        resource.setCategoryId(request.getCategoryId());
        resource.setDescription(request.getDescription());
        resource.setFileUrl(safeFileUrl);
        resource.setOwnerId(userId);
        resource.setOwnerName(username);
        resource.setLikeCount(0);
        resource.setDownloadCount(0);
        resource.setViewCount(0);
        resource.setVisibility(request.getVisibility() == null ? "VISIBLE" : request.getVisibility());
        resource.setAllowDownload(request.getAllowDownload() == null || request.getAllowDownload());

        resourceMapper.insert(resource);

        return resourceMapper.findById(resource.getId());
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "resourceList", allEntries = true)
    public Resource update(Long id, Long userId, boolean admin, ResourceRequest request) {
        Resource resource = resourceMapper.findById(id);
        if (resource == null) {
            throw new BusinessException("资料不存在");
        }

        if (!admin && !resource.getOwnerId().equals(userId)) {
            throw new BusinessException("无权操作该资料");
        }

        if (request.getCategoryId() != null && categoryMapper.findById(request.getCategoryId()) == null) {
            throw new BusinessException("分类不存在");
        }

        resource.setTitle(request.getTitle());
        resource.setCategoryId(request.getCategoryId());
        resource.setDescription(request.getDescription());

        if (request.getFileUrl() != null) {
            resource.setFileUrl(request.getFileUrl());
        }

        if (request.getAllowDownload() != null) {
            resource.setAllowDownload(request.getAllowDownload());
        }
        if (request.getVisibility() != null) {
            resource.setVisibility(request.getVisibility());
        }

        resourceMapper.update(resource);

        return resourceMapper.findById(id);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "resourceList", allEntries = true)
    public void delete(Long id, Long userId, boolean admin) {
        Resource resource = resourceMapper.findById(id);
        if (resource == null) {
            throw new BusinessException("资料不存在");
        }

        if (!admin && (resource.getOwnerId() == null || !resource.getOwnerId().equals(userId))) {
            throw new BusinessException("无权删除该资料");
        }

        punishmentMapper.deleteByResourceId(id);

        List<ResourceReport> reports = reportMapper.findByResourceId(id);
        for (ResourceReport report : reports) {
            punishmentMapper.deleteByReportId(report.getId());
        }

        favoriteMapper.deleteByResourceId(id);
        likeMapper.deleteByResourceId(id);
        reportMapper.deleteByResourceId(id);
        commentMapper.deleteByResourceId(id);

        if (resource.getFileUrl() != null && !resource.getFileUrl().isEmpty()) {
            fileStorageService.delete(resource.getFileUrl());
        }

        resourceMapper.delete(id);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "resourceList", allEntries = true)
    public Resource updateVisibility(Long id, Long userId, boolean admin, String visibility) {
        Resource resource = resourceMapper.findById(id);
        if (resource == null) {
            throw new BusinessException("资料不存在");
        }

        if (!admin && !resource.getOwnerId().equals(userId)) {
            throw new BusinessException("无权操作该资料");
        }

        if (visibility == null || visibility.isEmpty()) {
            throw new BusinessException("可见性参数不能为空");
        }
        if (!"VISIBLE".equalsIgnoreCase(visibility) && !"HIDDEN".equalsIgnoreCase(visibility)) {
            throw new BusinessException("可见性取值错误");
        }

        resourceMapper.updateVisibility(id, visibility);

        if (admin && "HIDDEN".equalsIgnoreCase(visibility)) {
            try {
                ResourceReport report = new ResourceReport();
                report.setResourceId(id);
                report.setUserId(userId);
                report.setReason("管理员直接隐藏资料");
                report.setStatus("RESOLVED");
                report.setReviewReply("管理员直接隐藏资料");
                report.setCancelled(false);
                report.setResourceTitle(resource.getTitle());
                report.setReviewedAt(LocalDateTime.now());
                reportMapper.insert(report);
            } catch (Exception e) {
                log.warn("管理员隐藏资料时创建举报记录失败，resourceId={}, adminId={}", id, userId, e);
            }
        }

        return resourceMapper.findById(id);
    }

    @Override
    @Transactional
    public void recordDownload(Long id) {
        if (resourceMapper.incrementDownload(id) == 0) {
            throw new BusinessException("记录下载失败");
        }
    }
}
