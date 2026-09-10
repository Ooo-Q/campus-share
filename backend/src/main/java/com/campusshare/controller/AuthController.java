package com.campusshare.controller;

import com.campusshare.dto.LoginRequest;
import com.campusshare.dto.PasswordChangeRequest;
import com.campusshare.dto.ProfileUpdateRequest;
import com.campusshare.dto.RegisterRequest;
import com.campusshare.dto.UsernameChangeRequest;
import com.campusshare.entity.User;
import com.campusshare.service.AuthService;
import com.campusshare.service.UserService;
import com.campusshare.util.SecurityUtil;
import com.campusshare.vo.ApiResponse;
import com.campusshare.vo.AuthResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusshare.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(
            @RequestBody(required = false) String body,
            LoginRequest formBody) {
        LoginRequest request;
        try {
            if (body != null && body.trim().startsWith("{")) {
                ObjectMapper mapper = new ObjectMapper();
                request = mapper.readValue(body, LoginRequest.class);
            } else if (formBody != null && formBody.getUsername() != null) {
                request = formBody;
            } else {
                throw new BusinessException("无法解析登录参数");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("登录参数格式错误");
        }
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @GetMapping("/me")
    public ApiResponse<User> profile() {
        Long userId = SecurityUtil.requireLogin();
        return ApiResponse.success(userService.getById(userId));
    }

    @PutMapping("/me")
    public ApiResponse<User> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        Long userId = SecurityUtil.requireLogin();
        return ApiResponse.success(userService.updateProfile(userId, request));
    }

    @PostMapping("/me/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        Long userId = SecurityUtil.requireLogin();
        userService.changePassword(userId, request);
        return ApiResponse.success("密码修改成功", null);
    }

    @PostMapping("/me/username")
    public ApiResponse<User> changeUsername(@Valid @RequestBody UsernameChangeRequest request) {
        Long userId = SecurityUtil.requireLogin();
        userService.changeUsername(userId, request);
        return ApiResponse.success(userService.getById(userId));
    }
}
