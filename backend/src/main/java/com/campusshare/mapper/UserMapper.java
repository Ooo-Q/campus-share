package com.campusshare.mapper;

import com.campusshare.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    User findById(@Param("id") Long id);

    User findByUsername(@Param("username") String username);

    List<User> search(@Param("keyword") String keyword, @Param("role") String role);

    int insert(User user);

    int update(User user);

    int delete(@Param("id") Long id);
}

