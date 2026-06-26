package com.mindvault.knowledge;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindvault.knowledge.entity.Knowledge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 知识数据访问层
 *
 * 继承 MyBatis-Plus BaseMapper 提供标准 CRUD，
 * 同时自定义 SQL 方法支持：
 * - 语义搜索（pgvector <=> 余弦距离）
 * - 关键词搜索（ILIKE + 排序）
 * - 标签搜索（JSONB @> 包含操作）
 * - 时间范围查询
 * - 标签聚合统计（UNNEST + GROUP BY）
 * - 嵌入向量完整性检查
 *
 * 注意：MyBatis-Plus 3.5.9 已移除 PaginationInnerInterceptor，
 * 分页在 Service 层通过 LIMIT/OFFSET 手动实现。
 */
@Mapper
public interface KnowledgeMapper extends BaseMapper<Knowledge> {

    /** 语义搜索：基于 pgvector 余弦距离计算相似度，返回 id + similarity 分数 */
    @Select(value = """
            SELECT id, 1 - (embedding <=> CAST(#{embedding} AS vector)) AS similarity
            FROM knowledge
            WHERE embedding IS NOT NULL
            ORDER BY embedding <=> CAST(#{embedding} AS vector)
            LIMIT #{topN}
            """)
    List<Map<String, Object>> findSimilarIds(@Param("embedding") String embedding, @Param("topN") int topN);

    /** 关键词搜索（带排名）：标题(权重2) > AI标题(权重1) > 内容(权重1) */
    @Select("""
            SELECT id, ROW_NUMBER() OVER (ORDER BY
                CASE WHEN title ILIKE CONCAT('%', #{query}, '%') THEN 2 ELSE 0 END
                + CASE WHEN ai_title ILIKE CONCAT('%', #{query}, '%') THEN 1 ELSE 0 END
                + CASE WHEN content ILIKE CONCAT('%', #{query}, '%') THEN 1 ELSE 0 END
                DESC) AS rank
            FROM knowledge
            WHERE content ILIKE CONCAT('%', #{query}, '%')
               OR title ILIKE CONCAT('%', #{query}, '%')
               OR ai_title ILIKE CONCAT('%', #{query}, '%')
            LIMIT #{limit}
            """)
    List<Map<String, Object>> keywordSearchWithRank(@Param("query") String query, @Param("limit") int limit);

    /** 关键词搜索（返回完整实体，按创建时间降序） */
    @Select("""
            SELECT * FROM knowledge
            WHERE content ILIKE CONCAT('%', #{query}, '%')
               OR title ILIKE CONCAT('%', #{query}, '%')
               OR ai_title ILIKE CONCAT('%', #{query}, '%')
            ORDER BY created_at DESC
            LIMIT #{limit}
            """)
    List<Knowledge> keywordSearch(@Param("query") String query, @Param("limit") int limit);

    /** 标签搜索：使用 JSONB @> 操作符匹配 ai_tags 或 user_tags */
    @Select("""
            SELECT * FROM knowledge
            WHERE (tags IS NOT NULL AND tags @> CAST(#{tagJson} AS jsonb))
               OR (user_tags IS NOT NULL AND user_tags @> CAST(#{tagJson} AS jsonb))
            ORDER BY created_at DESC
            LIMIT #{limit}
            """)
    List<Knowledge> searchByTag(@Param("tagJson") String tagJson, @Param("limit") int limit);

    /** 按创建时间范围查询（用于每日复盘、统计等） */
    @Select("SELECT * FROM knowledge WHERE created_at >= #{start} AND created_at < #{end} ORDER BY created_at DESC")
    List<Knowledge> findByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 查询是否有至少一条知识拥有嵌入向量（用于判断 pgvector 是否可用） */
    @Select("SELECT id FROM knowledge WHERE embedding IS NOT NULL LIMIT 1")
    Long findFirstWithEmbedding();

    /** 按标题/内容模糊匹配（用于导入时的冲突检测） */
    @Select("""
            SELECT * FROM knowledge
            WHERE LOWER(title) LIKE LOWER(CONCAT('%', #{title}, '%'))
               OR LOWER(ai_title) LIKE LOWER(CONCAT('%', #{title}, '%'))
               OR LOWER(content) LIKE LOWER(CONCAT('%', #{content}, '%'))
            LIMIT 20
            """)
    List<Knowledge> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(@Param("title") String title, @Param("content") String content);

    /** 统计缺少嵌入向量的知识条目数（向量一致性检查用） */
    @Select("""
            SELECT COUNT(*) FROM knowledge WHERE embedding IS NULL OR embedding = ''
            """)
    int countMissingEmbeddings();

    /** 统计处于指定自动处理状态的知识条目数 */
    @Select("SELECT COUNT(*) FROM knowledge WHERE auto_process_status = #{status}")
    int countByAutoProcessStatus(@Param("status") String status);

    /** 按自动处理状态分批查询（用于调度器轮询待处理条目） */
    @Select("SELECT * FROM knowledge WHERE auto_process_status = #{status} ORDER BY created_at ASC LIMIT #{limit}")
    List<Knowledge> findByAutoProcessStatus(@Param("status") String status, @Param("limit") int limit);

    /** 聚合所有标签（ai_tags + user_tags）并统计使用次数，返回前 50 个热门标签 */
    @Select("SELECT unnest(tags_arr) AS name, COUNT(*) AS count FROM (" +
            "SELECT ARRAY(SELECT jsonb_array_elements_text(tags::jsonb)) AS tags_arr FROM knowledge WHERE tags IS NOT NULL AND tags != '[]' " +
            "UNION ALL " +
            "SELECT ARRAY(SELECT jsonb_array_elements_text(user_tags::jsonb)) AS tags_arr FROM knowledge WHERE user_tags IS NOT NULL AND user_tags != '[]'" +
            ") t GROUP BY name ORDER BY count DESC LIMIT 50")
    List<Map<String, Object>> aggregateTags();
}
