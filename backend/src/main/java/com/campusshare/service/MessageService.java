package com.campusshare.service;

import com.campusshare.dto.SendMessageRequest;
import com.campusshare.vo.ConversationVO;
import com.campusshare.vo.MessageVO;

import java.util.List;

public interface MessageService {
    List<ConversationVO> getConversations(Long userId);

    List<MessageVO> getMessages(Long userId, Long otherUserId, Integer page, Integer size);

    MessageVO sendMessage(Long fromUserId, SendMessageRequest request);

    void markAsRead(Long userId, Long otherUserId);

    Integer getUnreadCount(Long userId);

    void deleteConversation(Long userId, Long otherUserId);
}
