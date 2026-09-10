package com.campusshare.service;

import com.campusshare.dto.CategoryRequest;
import com.campusshare.entity.ResourceCategory;

import java.util.List;

public interface CategoryService {
    List<ResourceCategory> listAll();

    ResourceCategory create(CategoryRequest request);

    ResourceCategory update(Long id, CategoryRequest request);

    void delete(Long id);
}
