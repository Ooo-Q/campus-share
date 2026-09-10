package com.campusshare.controller;

import com.campusshare.entity.Resource;
import com.campusshare.entity.ResourceFavorite;
import com.campusshare.service.FavoriteService;
import com.campusshare.util.SecurityUtil;
import com.campusshare.vo.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping
    public ApiResponse<List<ResourceFavorite>> list() {
        Long userId = SecurityUtil.requireLogin();
        return ApiResponse.success(favoriteService.listByUser(userId));
    }

    @PostMapping("/{resourceId}")
    public ApiResponse<Resource> toggle(@PathVariable Long resourceId) {
        Long userId = SecurityUtil.requireLogin();
        Resource resource = favoriteService.toggleFavorite(userId, resourceId);
        return ApiResponse.success("操作成功", resource);
    }
}
