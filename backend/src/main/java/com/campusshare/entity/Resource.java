package com.campusshare.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Resource {
    private Long id;

    private String title;

    private Long categoryId;

    private String categoryName;

    private String description;

    private String fileUrl;

    private Long ownerId;

    private String ownerName;

    private Integer likeCount;

    private Integer downloadCount;

    private Integer viewCount;

    private Integer favoriteCount;

    private String visibility;

    private Boolean allowDownload;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
