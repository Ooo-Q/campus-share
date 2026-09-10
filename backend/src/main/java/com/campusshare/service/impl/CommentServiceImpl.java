package com.campusshare.service.impl;

import com.campusshare.dto.CommentCreateRequest;
import com.campusshare.entity.Resource;
import com.campusshare.entity.ResourceComment;
import com.campusshare.entity.User;
import com.campusshare.exception.BusinessException;
import com.campusshare.mapper.ResourceCommentMapper;
import com.campusshare.mapper.ResourceMapper;
import com.campusshare.mapper.UserMapper;
import com.campusshare.service.CommentService;
import com.campusshare.vo.CommentVO;
import com.campusshare.vo.PageResponse;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final ResourceCommentMapper commentMapper;
    private final ResourceMapper resourceMapper;
    private final UserMapper userMapper;

    @Override
    public PageResponse<CommentVO> listComments(Long resourceId, Integer page, Integer size) {
        int pageNum = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : size;

        PageHelper.startPage(pageNum, pageSize);
        List<ResourceComment> roots = commentMapper.listRoots(resourceId);
        PageInfo<ResourceComment> pageInfo = new PageInfo<>(roots);

        List<Long> rootIds = roots.stream().map(ResourceComment::getId).collect(Collectors.toList());
        final Map<Long, List<ResourceComment>> childrenMap;
        if (!rootIds.isEmpty()) {
            List<ResourceComment> children = commentMapper.listByRootIds(rootIds);
            childrenMap = children.stream().collect(Collectors.groupingBy(ResourceComment::getParentId));
        } else {
            childrenMap = new HashMap<>();
        }

        List<CommentVO> records = roots.stream()
                .map(root -> toVO(root, childrenMap))
                .collect(Collectors.toList());

        return PageResponse.of(pageInfo.getTotal(), pageNum, pageSize, records);
    }

    private CommentVO toVO(ResourceComment comment, Map<Long, List<ResourceComment>> childrenMap) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setResourceId(comment.getResourceId());
        vo.setRootId(comment.getRootId());
        vo.setParentId(comment.getParentId());
        vo.setUserId(comment.getUserId());
        vo.setDisplayName(comment.getDisplayName());
        // 匿名评论 avatar 库中存空串；前端用本地默认匿名头像，避免写入超长 data URI
        vo.setAvatar(Boolean.TRUE.equals(comment.getIsAnonymous()) ? "" : comment.getAvatar());
        vo.setReplyToName(comment.getReplyToName());
        vo.setContent(comment.getContent());
        vo.setStatus(comment.getStatus());
        vo.setCreatedAt(comment.getCreatedAt());

        List<ResourceComment> children = childrenMap.getOrDefault(comment.getId(), new ArrayList<>());
        vo.setChildren(children.stream().map(child -> toVO(child, childrenMap)).collect(Collectors.toList()));
        return vo;
    }

    @Override
    @Transactional
    public CommentVO createComment(Long resourceId, Long userId, CommentCreateRequest request) {
        Resource resource = resourceMapper.findById(resourceId);
        if (resource == null) {
            throw new BusinessException("资料不存在");
        }

        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        ResourceComment parent = null;
        if (request.getParentId() != null) {
            parent = commentMapper.findById(request.getParentId());
            if (parent == null || !Objects.equals(parent.getResourceId(), resourceId)) {
                throw new BusinessException("父评论不存在");
            }
        }

        boolean anonymous = Boolean.TRUE.equals(request.getAnonymous());
        String displayName = anonymous ? "匿名用户" : (user.getNickname() != null ? user.getNickname() : user.getUsername());
        String avatar = anonymous ? "" : user.getAvatar();

        ResourceComment comment = new ResourceComment();
        comment.setResourceId(resourceId);
        comment.setParentId(request.getParentId());
        comment.setUserId(userId);
        comment.setIsAnonymous(anonymous);
        comment.setDisplayName(displayName);
        comment.setAvatar(avatar);
        comment.setContent(request.getContent());
        comment.setStatus("NORMAL");

        if (parent != null) {
            comment.setRootId(parent.getRootId() == null ? parent.getId() : parent.getRootId());
            comment.setReplyToUid(parent.getUserId());
            comment.setReplyToName(parent.getDisplayName());
        }

        commentMapper.insert(comment);

        if (comment.getRootId() == null) {
            comment.setRootId(comment.getId());
            commentMapper.updateRootId(comment.getId(), comment.getRootId());
        }

        return toVO(comment, new HashMap<>());
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId, boolean admin, Long resourceOwnerId) {
        ResourceComment comment = commentMapper.findById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        boolean canDelete = admin || 
                           (resourceOwnerId != null && Objects.equals(resourceOwnerId, userId)) ||
                           Objects.equals(comment.getUserId(), userId);
        if (!canDelete) {
            throw new BusinessException("无权删除该评论");
        }
        commentMapper.softDelete(commentId);
    }

    @Override
    @Transactional
    public void deleteByResource(Long resourceId) {
        commentMapper.deleteByResourceId(resourceId);
    }
}

