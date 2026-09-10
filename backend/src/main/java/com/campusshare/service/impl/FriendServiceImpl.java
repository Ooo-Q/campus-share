package com.campusshare.service.impl;

import com.campusshare.dto.FriendRequestDTO;
import com.campusshare.entity.FriendRequest;
import com.campusshare.entity.User;
import com.campusshare.entity.UserFriend;
import com.campusshare.exception.BusinessException;
import com.campusshare.mapper.FriendRequestMapper;
import com.campusshare.mapper.ResourceFavoriteMapper;
import com.campusshare.mapper.ResourceLikeMapper;
import com.campusshare.mapper.ResourceMapper;
import com.campusshare.mapper.UserFriendMapper;
import com.campusshare.mapper.UserMapper;
import com.campusshare.service.FriendService;
import com.campusshare.vo.FriendRequestVO;
import com.campusshare.vo.FriendVO;
import com.campusshare.vo.UserProfileVO;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {

    private final UserFriendMapper userFriendMapper;
    private final FriendRequestMapper friendRequestMapper;
    private final UserMapper userMapper;
    private final ResourceMapper resourceMapper;
    private final ResourceFavoriteMapper resourceFavoriteMapper;
    private final ResourceLikeMapper resourceLikeMapper;

    @Override
    public List<FriendVO> getFriendList(Long userId) {
        List<UserFriend> friends = userFriendMapper.findByUserId(userId, "FRIEND");
        return friends.stream().map(friend -> {
            User user = userMapper.findById(friend.getFriendId());
            if (user == null) return null;

            FriendVO vo = new FriendVO();
            vo.setId(friend.getId());
            vo.setUserId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
            vo.setStatus(friend.getStatus());
            vo.setCreatedAt(friend.getCreatedAt());
            return vo;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public List<FriendVO> getBlockedList(Long userId) {
        List<UserFriend> blocked = userFriendMapper.findByUserId(userId, "BLOCKED");
        return blocked.stream().map(friend -> {
            User user = userMapper.findById(friend.getFriendId());
            if (user == null) return null;
            FriendVO vo = new FriendVO();
            vo.setId(friend.getId());
            vo.setUserId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
            vo.setStatus(friend.getStatus());
            vo.setCreatedAt(friend.getCreatedAt());

            UserFriend reverseRelation = userFriendMapper.find(friend.getFriendId(), userId);
            vo.setHasFriendRelation(reverseRelation != null && "FRIEND".equals(reverseRelation.getStatus()));

            return vo;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void sendFriendRequest(Long fromUserId, FriendRequestDTO request) {
        if (Objects.equals(fromUserId, request.getToUserId())) {
            throw new BusinessException("不能添加自己为好友");
        }

        User toUser = userMapper.findById(request.getToUserId());
        if (toUser == null) {
            throw new BusinessException("用户不存在");
        }

        UserFriend existing = userFriendMapper.find(fromUserId, request.getToUserId());
        if (existing != null && "FRIEND".equals(existing.getStatus())) {
            throw new BusinessException("已经是好友了");
        }

        if (existing != null && "BLOCKED".equals(existing.getStatus())) {
            throw new BusinessException("你已拉黑该用户");
        }

        UserFriend blockedByOther = userFriendMapper.find(request.getToUserId(), fromUserId);
        if (blockedByOther != null && "BLOCKED".equals(blockedByOther.getStatus())) {
            throw new BusinessException("你已被对方拉黑，无法发送好友申请");
        }

        FriendRequest pending = friendRequestMapper.findPending(fromUserId, request.getToUserId());
        if (pending != null) {
            throw new BusinessException("已发送过好友申请，请等待对方处理");
        }

        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setFromUserId(fromUserId);
        friendRequest.setToUserId(request.getToUserId());
        friendRequest.setMessage(request.getMessage());
        friendRequest.setStatus("PENDING");
        friendRequestMapper.insert(friendRequest);
    }

    @Override
    public List<FriendRequestVO> getAllRequests(Long userId) {
        List<FriendRequest> receivedRequests = friendRequestMapper.findByToUserId(userId, null);
        List<FriendRequest> sentRequests = friendRequestMapper.findByFromUserId(userId);

        List<FriendRequestVO> allRequests = new ArrayList<>();

        for (FriendRequest request : receivedRequests) {
            User fromUser = userMapper.findById(request.getFromUserId());
            if (fromUser == null) continue;
            FriendRequestVO vo = new FriendRequestVO();
            vo.setId(request.getId());
            vo.setFromUserId(fromUser.getId());
            vo.setFromUsername(fromUser.getUsername());
            vo.setFromNickname(fromUser.getNickname());
            vo.setFromAvatar(fromUser.getAvatar());
            vo.setToUserId(userId);
            vo.setMessage(request.getMessage());
            vo.setStatus(request.getStatus());
            vo.setCreatedAt(request.getCreatedAt());
            vo.setUpdatedAt(request.getUpdatedAt());
            vo.setIsFromMe(false);
            allRequests.add(vo);
        }

        for (FriendRequest request : sentRequests) {
            User toUser = userMapper.findById(request.getToUserId());
            if (toUser == null) continue;
            FriendRequestVO vo = new FriendRequestVO();
            vo.setId(request.getId());
            vo.setFromUserId(userId);
            vo.setToUserId(toUser.getId());
            vo.setToUsername(toUser.getUsername());
            vo.setToNickname(toUser.getNickname());
            vo.setToAvatar(toUser.getAvatar());
            vo.setMessage(request.getMessage());
            vo.setStatus(request.getStatus());
            vo.setCreatedAt(request.getCreatedAt());
            vo.setUpdatedAt(request.getUpdatedAt());
            vo.setIsFromMe(true);
            allRequests.add(vo);
        }

        allRequests.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        return allRequests;
    }

    @Override
    @Transactional
    public void acceptFriendRequest(Long userId, Long requestId) {
        FriendRequest request = friendRequestMapper.findById(requestId);
        if (request == null) {
            throw new BusinessException("申请不存在");
        }
        if (!Objects.equals(request.getToUserId(), userId)) {
            throw new BusinessException("无权操作该申请");
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new BusinessException("申请已处理");
        }

        friendRequestMapper.updateStatus(requestId, "ACCEPTED");

        UserFriend friend1 = new UserFriend();
        friend1.setUserId(request.getFromUserId());
        friend1.setFriendId(request.getToUserId());
        friend1.setStatus("FRIEND");
        if (userFriendMapper.find(friend1.getUserId(), friend1.getFriendId()) == null) {
            userFriendMapper.insert(friend1);
        }

        UserFriend friend2 = new UserFriend();
        friend2.setUserId(request.getToUserId());
        friend2.setFriendId(request.getFromUserId());
        friend2.setStatus("FRIEND");
        if (userFriendMapper.find(friend2.getUserId(), friend2.getFriendId()) == null) {
            userFriendMapper.insert(friend2);
        }
    }

    @Override
    @Transactional
    public void rejectFriendRequest(Long userId, Long requestId) {
        FriendRequest request = friendRequestMapper.findById(requestId);
        if (request == null) {
            throw new BusinessException("申请不存在");
        }
        if (!Objects.equals(request.getToUserId(), userId)) {
            throw new BusinessException("无权操作该申请");
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new BusinessException("申请已处理");
        }
        friendRequestMapper.updateStatus(requestId, "REJECTED");
    }

    @Override
    @Transactional
    public void deleteRequest(Long userId, Long requestId) {
        FriendRequest request = friendRequestMapper.findById(requestId);
        if (request == null) {
            throw new BusinessException("申请不存在");
        }
        if (!Objects.equals(request.getFromUserId(), userId) && !Objects.equals(request.getToUserId(), userId)) {
            throw new BusinessException("无权删除该申请");
        }
        friendRequestMapper.delete(requestId);
    }

    @Override
    @Transactional
    public void deleteFriend(Long userId, Long friendId) {
        UserFriend blockedByOther = userFriendMapper.find(friendId, userId);
        boolean isBlockedByOther = (blockedByOther != null && "BLOCKED".equals(blockedByOther.getStatus()));

        UserFriend iBlockedOther = userFriendMapper.find(userId, friendId);
        boolean isFromBlockedList = (iBlockedOther != null && "BLOCKED".equals(iBlockedOther.getStatus()));

        if (!isFromBlockedList) {
            userFriendMapper.delete(userId, friendId);
        }

        if (!isBlockedByOther) {
            userFriendMapper.delete(friendId, userId);
        }
    }

    @Override
    @Transactional
    public void blockFriend(Long userId, Long friendId) {
        UserFriend existing = userFriendMapper.find(userId, friendId);
        if (existing == null || !"FRIEND".equals(existing.getStatus())) {
            throw new BusinessException("只能拉黑好友");
        }

        userFriendMapper.updateStatus(userId, friendId, "BLOCKED");
    }

    @Override
    @Transactional
    public void unblockFriend(Long userId, Long friendId) {
        UserFriend blocked = userFriendMapper.find(userId, friendId);
        if (blocked == null || !"BLOCKED".equals(blocked.getStatus())) {
            throw new BusinessException("该用户未被拉黑");
        }

        UserFriend reverseRelation = userFriendMapper.find(friendId, userId);
        if (reverseRelation != null && "FRIEND".equals(reverseRelation.getStatus())) {
            userFriendMapper.updateStatus(userId, friendId, "FRIEND");
        } else {
            userFriendMapper.delete(userId, friendId);
        }
    }

    @Override
    public UserProfileVO getUserProfile(Long currentUserId, Long targetUserId) {
        User user = userMapper.findById(targetUserId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        UserProfileVO vo = new UserProfileVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setGender(user.getGender());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setCreatedAt(user.getCreatedAt());

        vo.setResourceCount(resourceMapper.countByOwner(targetUserId));
        vo.setFavoriteCount(resourceFavoriteMapper.countByResourceOwner(targetUserId));
        vo.setLikeCount(resourceLikeMapper.countByResourceOwner(targetUserId));

        if (Objects.equals(currentUserId, targetUserId)) {
            vo.setFriendStatus("SELF");
        } else {
            UserFriend friend = userFriendMapper.find(currentUserId, targetUserId);
            if (friend != null) {
                if ("FRIEND".equals(friend.getStatus())) {
                    vo.setFriendStatus("FRIEND");
                } else if ("BLOCKED".equals(friend.getStatus())) {
                    vo.setFriendStatus("BLOCKED");
                }
            } else {
                FriendRequest pending = friendRequestMapper.findPending(currentUserId, targetUserId);
                if (pending != null) {
                    vo.setFriendStatus("PENDING");
                    vo.setFriendRequestId(pending.getId());
                } else {
                    vo.setFriendStatus("NONE");
                }
            }
        }

        return vo;
    }

    @Override
    public List<UserProfileVO> searchUsers(String keyword, Integer page, Integer size) {
        int pageNum = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : size;

        PageHelper.startPage(pageNum, pageSize);
        List<User> users = userMapper.search(keyword, null);

        return users.stream().map(user -> {
            UserProfileVO vo = new UserProfileVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
            vo.setCreatedAt(user.getCreatedAt());
            vo.setResourceCount(resourceMapper.countByOwner(user.getId()));
            return vo;
        }).collect(Collectors.toList());
    }
}
