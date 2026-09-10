package com.campusshare.service;

import com.campusshare.dto.PasswordChangeRequest;
import com.campusshare.dto.ProfileUpdateRequest;
import com.campusshare.dto.UsernameChangeRequest;
import com.campusshare.entity.User;
import com.campusshare.vo.PageResponse;

public interface UserService {
    User getById(Long id);

    PageResponse<User> pageList(Integer page, Integer size, String keyword, String role);

    User updateProfile(Long userId, ProfileUpdateRequest request);

    void changePassword(Long userId, PasswordChangeRequest request);

    void changeUsername(Long userId, UsernameChangeRequest request);

    User updateByAdmin(Long userId, String username, String password, String nickname, String gender, String phone, String email, String avatar);

    User createByAdmin(String username, String password, String nickname, String gender, String phone, String email, String avatar);

    void delete(Long id);
}
