package com.campusshare.service;

import com.campusshare.dto.CommentCreateRequest;
import com.campusshare.vo.CommentVO;
import com.campusshare.vo.PageResponse;

public interface CommentService {

    PageResponse<CommentVO> listComments(Long resourceId, Integer page, Integer size);

    CommentVO createComment(Long resourceId, Long userId, CommentCreateRequest request);

    void deleteComment(Long commentId, Long userId, boolean admin, Long resourceOwnerId);

    void deleteByResource(Long resourceId);
}


