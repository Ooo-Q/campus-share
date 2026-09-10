package com.campusshare.controller;

import com.campusshare.dto.CommentCreateRequest;
import com.campusshare.entity.Resource;
import com.campusshare.mapper.ResourceMapper;
import com.campusshare.security.UserContext;
import com.campusshare.service.CommentService;
import com.campusshare.util.SecurityUtil;
import com.campusshare.vo.ApiResponse;
import com.campusshare.vo.CommentVO;
import com.campusshare.vo.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/resources/{resourceId:\\d+}/comments")
public class CommentController {

    private final CommentService commentService;
    private final ResourceMapper resourceMapper;

    @GetMapping
    public ApiResponse<PageResponse<CommentVO>> list(@PathVariable Long resourceId,
                                                     @RequestParam(defaultValue = "1") Integer page,
                                                     @RequestParam(defaultValue = "10") Integer size) {
        return ApiResponse.success(commentService.listComments(resourceId, page, size));
    }

    @PostMapping
    public ApiResponse<CommentVO> create(@PathVariable Long resourceId,
                                         @Valid @RequestBody CommentCreateRequest request) {
        Long userId = SecurityUtil.requireLogin();
        CommentVO vo = commentService.createComment(resourceId, userId, request);
        return ApiResponse.success("发布成功", vo);
    }

    @DeleteMapping("/{commentId}")
    public ApiResponse<Void> delete(@PathVariable Long resourceId, @PathVariable Long commentId) {
        Long userId = SecurityUtil.requireLogin();
        boolean admin = "ADMIN".equalsIgnoreCase(UserContext.getRole());
        Resource resource = resourceMapper.findById(resourceId);
        Long resourceOwnerId = resource != null ? resource.getOwnerId() : null;
        commentService.deleteComment(commentId, userId, admin, resourceOwnerId);
        return ApiResponse.success("删除成功", null);
    }
}


