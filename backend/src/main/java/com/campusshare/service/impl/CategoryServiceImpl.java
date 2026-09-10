package com.campusshare.service.impl;

import com.campusshare.dto.CategoryRequest;
import com.campusshare.entity.ResourceCategory;
import com.campusshare.exception.BusinessException;
import com.campusshare.mapper.ResourceCategoryMapper;
import com.campusshare.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final ResourceCategoryMapper categoryMapper;

    @Override
    public List<ResourceCategory> listAll() {
        return categoryMapper.findAll();
    }

    @Override
    @Transactional
    public ResourceCategory create(CategoryRequest request) {
        ResourceCategory category = new ResourceCategory();
        category.setName(request.getName());
        category.setSortOrder(request.getSortOrder());

        categoryMapper.insert(category);

        return category;
    }

    @Override
    @Transactional
    public ResourceCategory update(Long id, CategoryRequest request) {
        ResourceCategory category = categoryMapper.findById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }

        category.setName(request.getName());
        category.setSortOrder(request.getSortOrder());

        categoryMapper.update(category);

        return category;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (categoryMapper.delete(id) == 0) {
            throw new BusinessException("删除分类失败");
        }
    }
}
