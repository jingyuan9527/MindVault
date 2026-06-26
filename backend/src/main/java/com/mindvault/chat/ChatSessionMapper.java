package com.mindvault.chat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindvault.chat.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 聊天会话数据访问层。
 * <p>继承 MyBatis-Plus BaseMapper 提供标准 CRUD。
 * 自定义查询 {@link #findAllByOrderByUpdatedAtDesc()} 用于会话列表页，
 * 按更新时间降序排列，使最近活跃的会话排在最前。</p>
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {

    /**
     * 查询所有会话，按更新时间降序排列。
     * @return 会话列表，最近更新的排在最前
     */
    @Select("SELECT * FROM chat_session ORDER BY updated_at DESC")
    List<ChatSession> findAllByOrderByUpdatedAtDesc();
}