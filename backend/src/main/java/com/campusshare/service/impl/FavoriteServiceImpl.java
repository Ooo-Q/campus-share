package com.campusshare.service.impl;

import com.campusshare.entity.Resource;
import com.campusshare.entity.ResourceFavorite;
import com.campusshare.exception.BusinessException;
import com.campusshare.mapper.ResourceFavoriteMapper;
import com.campusshare.mapper.ResourceMapper;
import com.campusshare.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final ResourceFavoriteMapper favoriteMapper;
    private final ResourceMapper resourceMapper;

    @Override
    public List<ResourceFavorite> listByUser(Long userId) {
        return favoriteMapper.findByUser(userId);
    }

    @Override
    @Transactional
    public Resource toggleFavorite(Long userId, Long resourceId) {
        Resource resource = resourceMapper.findById(resourceId);
        if (resource == null) {
            throw new BusinessException("资料不存在");
        }
        ResourceFavorite existing = favoriteMapper.find(userId, resourceId);
        if (existing != null) {
            favoriteMapper.delete(existing.getId());
        } else {
            ResourceFavorite favorite = new ResourceFavorite();
            favorite.setUserId(userId);
            favorite.setResourceId(resourceId);
            favoriteMapper.insert(favorite);
        }
        return resourceMapper.findById(resourceId);
    }
}
