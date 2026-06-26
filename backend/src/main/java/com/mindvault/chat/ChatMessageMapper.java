package com.mindvault.chat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindvault.chat.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 聊天消息数据访问层。
 * <p>继承 MyBatis-Plus BaseMapper 提供标准 CRUD。
 * 自定义查询 {@link #findBySessionIdOrderByCreatedAtAsc} 按会话 ID 获取消息历史，
 * 按创建时间升序排列以还原对话顺序。</p>
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 查询指定会话的所有消息，按创建时间升序排列。
     * @param sessionId 会话 ID
     * @return 消息列表，从最早到最新
     */
    @Select("SELECT * FROM chat_message WHERE session_id = #{sessionId} ORDER BY created_at ASC")
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(@Param("sessionId") Long sessionId);
}