package com.campusshare.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsernameChangeRequest {
    @NotBlank(message = "新用户名不能为空")
    private String newUsername;
}
