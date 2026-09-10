package com.campusshare.service;

import com.campusshare.dto.FriendRequestDTO;
import com.campusshare.vo.FriendRequestVO;
import com.campusshare.vo.FriendVO;
import com.campusshare.vo.UserProfileVO;

import java.util.List;

public interface FriendService {
    List<FriendVO> getFriendList(Long userId);
    
    List<FriendVO> getBlockedList(Long userId);
    
    void sendFriendRequest(Long fromUserId, FriendRequestDTO request);

    List<FriendRequestVO> getAllRequests(Long userId);
    
    void acceptFriendRequest(Long userId, Long requestId);
    
    void rejectFriendRequest(Long userId, Long requestId);
    
    void deleteRequest(Long userId, Long requestId);
    
    void deleteFriend(Long userId, Long friendId);
    
    void blockFriend(Long userId, Long friendId);
    
    void unblockFriend(Long userId, Long friendId);
    
    UserProfileVO getUserProfile(Long currentUserId, Long targetUserId);
    
    List<UserProfileVO> searchUsers(String keyword, Integer page, Integer size);
}

