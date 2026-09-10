package com.campusshare.mapper;

import com.campusshare.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageMapper {
    Message findById(@Param("id") Long id);

    List<Message> findByConversation(@Param("userId1") Long userId1, @Param("userId2") Long userId2, @Param("limit") Integer limit, @Param("offset") Integer offset);

    List<Long> findConversationUserIds(@Param("userId") Long userId);

    int insert(Message message);

    int markAsRead(@Param("fromUserId") Long fromUserId, @Param("toUserId") Long toUserId);

    int countUnread(@Param("userId") Long userId);

    int countUnreadByConversation(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);

    int deleteByUserId(@Param("userId") Long userId);

    int deleteConversation(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}
