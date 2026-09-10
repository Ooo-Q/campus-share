package com.campusshare.service;

import com.campusshare.entity.Resource;
import com.campusshare.entity.ResourceFavorite;

import java.util.List;

public interface FavoriteService {
    List<ResourceFavorite> listByUser(Long userId);

    Resource toggleFavorite(Long userId, Long resourceId);
}
