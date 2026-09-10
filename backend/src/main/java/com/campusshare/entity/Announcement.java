package com.campusshare.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Announcement {
    private Long id;

    private String title;

    private String summary;

    private String content;

    private Boolean pinned;

    private LocalDateTime publishAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
