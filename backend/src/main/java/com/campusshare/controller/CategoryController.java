package com.campusshare.controller;

import com.campusshare.dto.CategoryRequest;
import com.campusshare.entity.ResourceCategory;
import com.campusshare.service.CategoryService;
import com.campusshare.util.SecurityUtil;
import com.campusshare.vo.ApiResponse;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<List<ResourceCategory>> list() {
        return ApiResponse.success(categoryService.listAll());
    }

    @PostMapping
    public ApiResponse<ResourceCategory> create(@Valid @RequestBody CategoryRequest request) {
        SecurityUtil.requireAdmin();
        return ApiResponse.success(categoryService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ResourceCategory> update(@PathVariable Long id,
                                                @Valid @RequestBody CategoryRequest request) {
        SecurityUtil.requireAdmin();
        return ApiResponse.success(categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        SecurityUtil.requireAdmin();
        categoryService.delete(id);
        return ApiResponse.success("删除成功", null);
    }
}
