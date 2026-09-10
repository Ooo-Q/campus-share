package com.campusshare.service.impl;

import com.campusshare.dto.SendMessageRequest;
import com.campusshare.entity.Message;
import com.campusshare.entity.User;
import com.campusshare.entity.UserFriend;
import com.campusshare.exception.BusinessException;
import com.campusshare.mapper.MessageMapper;
import com.campusshare.mapper.UserFriendMapper;
import com.campusshare.mapper.UserMapper;
import com.campusshare.service.MessageService;
import com.campusshare.service.PunishmentService;
import com.campusshare.vo.ConversationVO;
import com.campusshare.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final UserFriendMapper userFriendMapper;
    private final PunishmentService punishmentService;

    @Override
    public List<ConversationVO> getConversations(Long userId) {
        List<Long> conversationUserIds = messageMapper.findConversationUserIds(userId);
        List<ConversationVO> conversations = new ArrayList<>();

        for (Long otherUserId : conversationUserIds) {
            User otherUser = userMapper.findById(otherUserId);
            if (otherUser == null) continue;

            ConversationVO vo = new ConversationVO();
            vo.setUserId(otherUser.getId());
            vo.setUsername(otherUser.getUsername());
            vo.setNickname(otherUser.getNickname());
            vo.setAvatar(otherUser.getAvatar());

            List<Message> messages = messageMapper.findByConversation(userId, otherUserId, 1, 0);
            if (!messages.isEmpty()) {
                Message lastMessage = messages.get(0);
                vo.setLastMessage(lastMessage.getContent());
                vo.setLastMessageTime(lastMessage.getCreatedAt());
            }

            int unreadCount = messageMapper.countUnreadByConversation(userId, otherUserId);
            vo.setUnreadCount(unreadCount);

            conversations.add(vo);
        }

        return conversations;
    }

    @Override
    public List<MessageVO> getMessages(Long userId, Long otherUserId, Integer page, Integer size) {
        int pageNum = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 20 : size;
        int offset = (pageNum - 1) * pageSize;

        List<Message> messages = messageMapper.findByConversation(userId, otherUserId, pageSize, offset);

        messageMapper.markAsRead(otherUserId, userId);

        User otherUser = userMapper.findById(otherUserId);
        if (otherUser == null) {
            throw new BusinessException("用户不存在");
        }

        return messages.stream().map(message -> {
            MessageVO vo = new MessageVO();
            vo.setId(message.getId());
            vo.setFromUserId(message.getFromUserId());
            vo.setToUserId(message.getToUserId());
            vo.setContent(message.getContent());
            vo.setIsRead(message.getIsRead());
            vo.setCreatedAt(message.getCreatedAt());

            if (Objects.equals(message.getFromUserId(), otherUserId)) {
                vo.setFromUsername(otherUser.getUsername());
                vo.setFromNickname(otherUser.getNickname());
                vo.setFromAvatar(otherUser.getAvatar());
            } else {
                User currentUser = userMapper.findById(userId);
                if (currentUser != null) {
                    vo.setFromUsername(currentUser.getUsername());
                    vo.setFromNickname(currentUser.getNickname());
                    vo.setFromAvatar(currentUser.getAvatar());
                }
            }

            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MessageVO sendMessage(Long fromUserId, SendMessageRequest request) {
        if (punishmentService.isMuted(fromUserId)) {
            throw new BusinessException("你已被禁言，无法发送消息");
        }

        UserFriend blocked = userFriendMapper.find(request.getToUserId(), fromUserId);
        if (blocked != null && "BLOCKED".equals(blocked.getStatus())) {
            throw new BusinessException("你已被对方拉黑，无法发送消息");
        }

        UserFriend iBlocked = userFriendMapper.find(fromUserId, request.getToUserId());
        if (iBlocked != null && "BLOCKED".equals(iBlocked.getStatus())) {
            throw new BusinessException("你已拉黑该用户，无法发送消息");
        }

        UserFriend friendRelation = userFriendMapper.find(fromUserId, request.getToUserId());
        if (friendRelation == null || !"FRIEND".equals(friendRelation.getStatus())) {
            throw new BusinessException("你们不是好友关系，无法发送消息");
        }

        User toUser = userMapper.findById(request.getToUserId());
        if (toUser == null) {
            throw new BusinessException("用户不存在");
        }

        Message message = new Message();
        message.setFromUserId(fromUserId);
        message.setToUserId(request.getToUserId());
        message.setContent(request.getContent());
        message.setIsRead(false);
        messageMapper.insert(message);

        User fromUser = userMapper.findById(fromUserId);
        MessageVO vo = new MessageVO();
        vo.setId(message.getId());
        vo.setFromUserId(fromUserId);
        vo.setToUserId(request.getToUserId());
        vo.setContent(message.getContent());
        vo.setIsRead(false);
        vo.setCreatedAt(message.getCreatedAt());
        if (fromUser != null) {
            vo.setFromUsername(fromUser.getUsername());
            vo.setFromNickname(fromUser.getNickname());
            vo.setFromAvatar(fromUser.getAvatar());
        }

        return vo;
    }

    @Override
    @Transactional
    public void markAsRead(Long userId, Long otherUserId) {
        messageMapper.markAsRead(otherUserId, userId);
    }

    @Override
    public Integer getUnreadCount(Long userId) {
        return messageMapper.countUnread(userId);
    }

    @Override
    @Transactional
    public void deleteConversation(Long userId, Long otherUserId) {
        messageMapper.deleteConversation(userId, otherUserId);
    }
}
