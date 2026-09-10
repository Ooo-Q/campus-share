package com.campusshare.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementRequest {
    @NotBlank
    private String title;

    @NotBlank
    private String summary;

    @NotBlank
    private String content;

    private Boolean pinned = false;
}
