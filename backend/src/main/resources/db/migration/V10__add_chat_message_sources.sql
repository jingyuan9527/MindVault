-- V10__add_chat_message_sources.sql
-- 聊天消息增加 sources 字段，用于存储引用来源
ALTER TABLE chat_message ADD COLUMN IF NOT EXISTS sources JSONB DEFAULT '[]'::jsonb;