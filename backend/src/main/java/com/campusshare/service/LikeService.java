package com.campusshare.service;

import com.campusshare.entity.Resource;

public interface LikeService {
    Resource toggleLike(Long userId, Long resourceId);

    boolean isLiked(Long userId, Long resourceId);
}
