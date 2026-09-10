package com.campusshare.controller;

import com.campusshare.dto.AdminUpdateStudentRequest;
import com.campusshare.entity.User;
import com.campusshare.service.AdminService;
import com.campusshare.service.UserService;
import com.campusshare.util.SecurityUtil;
import com.campusshare.vo.AdminOverviewResponse;
import com.campusshare.vo.ApiResponse;
import com.campusshare.vo.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;

    @GetMapping("/overview")
    public ApiResponse<AdminOverviewResponse> overview() {
        SecurityUtil.requireAdmin();
        return ApiResponse.success(adminService.overview());
    }

    @GetMapping("/students")
    public ApiResponse<PageResponse<User>> listStudents(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role) {
        SecurityUtil.requireAdmin();
        PageResponse<User> data = userService.pageList(page, size, keyword, role);
        return ApiResponse.success(data);
    }

    @PostMapping("/students")
    public ApiResponse<User> createStudent(@Valid @RequestBody AdminCreateStudentRequest request) {
        SecurityUtil.requireAdmin();
        User created = userService.createByAdmin(
            request.getUsername(),
            request.getPassword(),
            request.getNickname(),
            request.getGender(),
            request.getPhone(),
            request.getEmail(),
            request.getAvatar()
        );
        return ApiResponse.success(created);
    }

    @PutMapping("/students/{id}")
    public ApiResponse<User> updateStudent(@PathVariable Long id,
                                          @Valid @RequestBody AdminUpdateStudentRequest request) {
        SecurityUtil.requireAdmin();
        User updated = userService.updateByAdmin(
            id,
            request.getUsername(),
            request.getPassword(),
            request.getNickname(),
            request.getGender(),
            request.getPhone(),
            request.getEmail(),
            request.getAvatar()
        );
        return ApiResponse.success(updated);
    }

    @DeleteMapping("/students/{id}")
    public ApiResponse<Void> deleteStudent(@PathVariable Long id) {
        SecurityUtil.requireAdmin();
        userService.delete(id);
        return ApiResponse.success("删除成功", null);
    }

    @DeleteMapping("/students/batch")
    public ApiResponse<Void> batchDeleteStudents(@RequestBody BatchDeleteRequest request) {
        SecurityUtil.requireAdmin();
        for (Long id : request.getIds()) {
            userService.delete(id);
        }
        return ApiResponse.success("批量删除成功", null);
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BatchDeleteRequest {
        private List<Long> ids;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AdminCreateStudentRequest {
        private String username;
        private String password;
        private String nickname;
        private String gender;
        private String phone;
        private String email;
        private String avatar;
    }
}

