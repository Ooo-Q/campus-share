package com.campusshare.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class CommentVO {
    private Long id;
    private Long resourceId;
    private Long rootId;
    private Long parentId;
    private Long userId;
    private String displayName;
    private String avatar;
    private String replyToName;
    private String content;
    private String status;
    private LocalDateTime createdAt;
    private List<CommentVO> children = new ArrayList<>();
}
