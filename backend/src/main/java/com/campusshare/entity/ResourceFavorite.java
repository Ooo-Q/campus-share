package com.campusshare.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResourceFavorite {
    private Long id;

    private Long resourceId;

    private Long userId;

    private LocalDateTime createdAt;
}
