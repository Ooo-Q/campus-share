package com.campusshare.controller;

import com.campusshare.dto.AnnouncementRequest;
import com.campusshare.entity.Announcement;
import com.campusshare.service.AnnouncementService;
import com.campusshare.util.SecurityUtil;
import com.campusshare.vo.ApiResponse;
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

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping
    public ApiResponse<List<Announcement>> list(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(announcementService.list(keyword));
    }

    @PostMapping
    public ApiResponse<Announcement> create(@Valid @RequestBody AnnouncementRequest request) {
        SecurityUtil.requireAdmin();
        return ApiResponse.success(announcementService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Announcement> update(@PathVariable Long id,
                                            @Valid @RequestBody AnnouncementRequest request) {
        SecurityUtil.requireAdmin();
        return ApiResponse.success(announcementService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        SecurityUtil.requireAdmin();
        announcementService.delete(id);
        return ApiResponse.success("删除成功", null);
    }
}
