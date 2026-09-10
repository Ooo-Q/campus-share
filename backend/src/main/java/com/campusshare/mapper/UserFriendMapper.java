package com.campusshare.mapper;

import com.campusshare.entity.UserFriend;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserFriendMapper {
    UserFriend find(@Param("userId") Long userId, @Param("friendId") Long friendId);
    
    List<UserFriend> findByUserId(@Param("userId") Long userId, @Param("status") String status);
    
    int insert(UserFriend userFriend);
    
    int updateStatus(@Param("userId") Long userId, @Param("friendId") Long friendId, @Param("status") String status);
    
    int delete(@Param("userId") Long userId, @Param("friendId") Long friendId);
    
    int deleteByUserId(@Param("userId") Long userId);
}

