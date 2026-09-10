package com.campusshare.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResourceComment {
    private Long id;
    private Long resourceId;
    private Long rootId;
    private Long parentId;
    private Long userId;
    private Boolean isAnonymous;
    private String displayName;
    private String avatar;
    private Long replyToUid;
    private String replyToName;
    private String content;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
