package com.campusshare.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResourceCategory {
    private Long id;

    private String name;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
