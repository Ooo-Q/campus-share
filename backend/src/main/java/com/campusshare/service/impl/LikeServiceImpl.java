package com.campusshare.service.impl;

import com.campusshare.entity.Resource;
import com.campusshare.entity.ResourceLike;
import com.campusshare.exception.BusinessException;
import com.campusshare.mapper.ResourceLikeMapper;
import com.campusshare.mapper.ResourceMapper;
import com.campusshare.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final ResourceLikeMapper likeMapper;
    private final ResourceMapper resourceMapper;

    @Override
    @Transactional
    public Resource toggleLike(Long userId, Long resourceId) {
        try {
            Resource resource = resourceMapper.findById(resourceId);
            if (resource == null) {
                throw new BusinessException("资料不存在");
            }
            ResourceLike existing = likeMapper.find(userId, resourceId);
            if (existing != null) {
                likeMapper.delete(existing.getId());
                if (resource.getLikeCount() != null && resource.getLikeCount() > 0) {
                    resource.setLikeCount(resource.getLikeCount() - 1);
                    resourceMapper.update(resource);
                }
            } else {
                ResourceLike like = new ResourceLike();
                like.setUserId(userId);
                like.setResourceId(resourceId);
                likeMapper.insert(like);
                resource.setLikeCount((resource.getLikeCount() == null ? 0 : resource.getLikeCount()) + 1);
                resourceMapper.update(resource);
            }
            return resourceMapper.findById(resourceId);
        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            String message = e.getMessage();
            if (message != null && message.contains("resource_like")) {
                throw new BusinessException("点赞功能需要创建数据库表，请执行 SQL: CREATE TABLE resource_like ...");
            }
            throw new BusinessException("点赞操作失败: " + (message != null ? message : e.getClass().getSimpleName()));
        }
    }

    @Override
    public boolean isLiked(Long userId, Long resourceId) {
        try {
            return likeMapper.find(userId, resourceId) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
