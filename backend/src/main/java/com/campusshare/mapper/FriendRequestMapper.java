package com.campusshare.mapper;

import com.campusshare.entity.FriendRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FriendRequestMapper {
    FriendRequest findById(@Param("id") Long id);
    
    FriendRequest findPending(@Param("fromUserId") Long fromUserId, @Param("toUserId") Long toUserId);
    
    List<FriendRequest> findByToUserId(@Param("toUserId") Long toUserId, @Param("status") String status);
    
    List<FriendRequest> findByFromUserId(@Param("fromUserId") Long fromUserId);
    
    int insert(FriendRequest friendRequest);
    
    int updateStatus(@Param("id") Long id, @Param("status") String status);
    
    int delete(@Param("id") Long id);
    
    int deleteByUserId(@Param("userId") Long userId);
}

