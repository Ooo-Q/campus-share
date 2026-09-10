package com.campusshare.controller;

import com.campusshare.dto.ResourceRequest;
import com.campusshare.entity.Resource;
import com.campusshare.exception.BusinessException;
import com.campusshare.mapper.ResourceFavoriteMapper;
import com.campusshare.security.UserContext;
import com.campusshare.service.LikeService;
import com.campusshare.service.ResourceService;
import com.campusshare.util.SecurityUtil;
import com.campusshare.vo.ApiResponse;
import com.campusshare.vo.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;
    private final LikeService likeService;
    private final ResourceFavoriteMapper favoriteMapper;
    private final com.campusshare.service.PunishmentService punishmentService;

    @GetMapping
    public ApiResponse<PageResponse<Resource>> page(@RequestParam(defaultValue = "1") Integer page,
                                                    @RequestParam(defaultValue = "10") Integer size,
                                                    @RequestParam(required = false) Long categoryId,
                                                    @RequestParam(required = false) String keyword) {
        PageResponse<Resource> data = resourceService.pageList(page, size, categoryId, keyword, "VISIBLE", null);
        return ApiResponse.success(data);
    }

    @GetMapping("/my")
    public ApiResponse<PageResponse<Resource>> pageForMy(@RequestParam(defaultValue = "1") Integer page,
                                                          @RequestParam(defaultValue = "10") Integer size,
                                                          @RequestParam(required = false) Long categoryId,
                                                          @RequestParam(required = false) String keyword) {
        Long userId = SecurityUtil.requireLogin();
        PageResponse<Resource> data = resourceService.pageList(page, size, categoryId, keyword, null, userId);
        return ApiResponse.success(data);
    }

    @GetMapping("/manage")
    public ApiResponse<PageResponse<Resource>> pageForOwner(@RequestParam(defaultValue = "1") Integer page,
                                                            @RequestParam(defaultValue = "10") Integer size,
                                                            @RequestParam(required = false) Long categoryId,
                                                            @RequestParam(required = false) String keyword) {
        Long userId = SecurityUtil.requireLogin();
        boolean admin = "ADMIN".equalsIgnoreCase(UserContext.get().getRole());
        Long owner = admin ? null : userId;
        PageResponse<Resource> data = resourceService.pageList(page, size, categoryId, keyword, null, owner);
        return ApiResponse.success(data);
    }

    @GetMapping("/{id:\\d+}")
    public ApiResponse<Resource> detail(@PathVariable Long id) {
        Resource resource = resourceService.getDetail(id, false);
        boolean isHidden = "HIDDEN".equalsIgnoreCase(resource.getVisibility());
        if (isHidden) {
            Long currentUserId = UserContext.get() == null ? null : UserContext.get().getId();
            boolean admin = UserContext.get() != null && "ADMIN".equalsIgnoreCase(UserContext.get().getRole());
            boolean isOwner = currentUserId != null && resource.getOwnerId().equals(currentUserId);
            boolean isFavorited = currentUserId != null && favoriteMapper.find(currentUserId, id) != null;
            if (!admin && !isOwner && !isFavorited) {
                throw new BusinessException("资料已被隐藏");
            }
        }
        resource = resourceService.getDetail(id, true);
        return ApiResponse.success(resource);
    }

    @PostMapping
    public ApiResponse<Resource> create(@Valid @RequestBody ResourceRequest request) {
        Long userId = SecurityUtil.requireLogin();
        punishmentService.checkUploadAllowed(userId);
        String username = UserContext.get().getUsername();
        return ApiResponse.success(resourceService.create(userId, username, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Resource> update(@PathVariable Long id,
                                        @Valid @RequestBody ResourceRequest request) {
        Long userId = SecurityUtil.requireLogin();
        boolean admin = "ADMIN".equalsIgnoreCase(UserContext.get().getRole());
        return ApiResponse.success(resourceService.update(id, userId, admin, request));
    }

    @PutMapping("/{id}/visibility")
    public ApiResponse<Resource> updateVisibility(@PathVariable Long id, @RequestParam String visibility) {
        Long userId = SecurityUtil.requireLogin();
        boolean admin = "ADMIN".equalsIgnoreCase(UserContext.get().getRole());
        return ApiResponse.success(resourceService.updateVisibility(id, userId, admin, visibility));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Long userId = SecurityUtil.requireLogin();
        boolean admin = "ADMIN".equalsIgnoreCase(UserContext.get().getRole());
        resourceService.delete(id, userId, admin);
        return ApiResponse.success("删除成功", null);
    }

    @DeleteMapping("/batch")
    public ApiResponse<Void> batchDelete(@RequestBody BatchDeleteRequest request) {
        SecurityUtil.requireAdmin();
        for (Long id : request.getIds()) {
            resourceService.delete(id, null, true);
        }
        return ApiResponse.success("批量删除成功", null);
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BatchDeleteRequest {
        private java.util.List<Long> ids;
    }

    @PostMapping("/{id}/like")
    public ApiResponse<Resource> like(@PathVariable Long id) {
        Long userId = SecurityUtil.requireLogin();
        Resource resource = likeService.toggleLike(userId, id);
        boolean isLiked = likeService.isLiked(userId, id);
        return ApiResponse.success(isLiked ? "点赞成功" : "取消点赞成功", resource);
    }

    @GetMapping("/{id}/like/status")
    public ApiResponse<Boolean> getLikeStatus(@PathVariable Long id) {
        Long userId = SecurityUtil.requireLogin();
        boolean isLiked = likeService.isLiked(userId, id);
        return ApiResponse.success(isLiked);
    }

    @PostMapping("/{id}/download")
    public ApiResponse<Void> recordDownload(@PathVariable Long id) {
        SecurityUtil.requireLogin();
        resourceService.recordDownload(id);
        return ApiResponse.success("记录成功", null);
    }
}
