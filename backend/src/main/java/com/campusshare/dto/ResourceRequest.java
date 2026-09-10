package com.campusshare.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceRequest {
    private String title;

    private Long categoryId;

    private String description;

    private String fileUrl;

    private Boolean allowDownload = true;

    private String visibility;
}
