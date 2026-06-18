package com.mindvault.chat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindvault.chat.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {

    @Select("SELECT * FROM chat_session ORDER BY updated_at DESC")
    List<ChatSession> findAllByOrderByUpdatedAtDesc();
}