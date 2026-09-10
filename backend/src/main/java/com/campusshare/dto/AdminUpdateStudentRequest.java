package com.campusshare.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpdateStudentRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;

    private String password;

    private String nickname;

    private String gender;

    private String phone;

    private String email;

    private String avatar;
}
