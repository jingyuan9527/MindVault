package com.mindvault.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindvault.knowledge.entity.Knowledge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface KnowledgeMapper extends BaseMapper<Knowledge> {

    @Select(value = """
            SELECT id, 1 - (embedding <=> CAST(#{embedding} AS vector)) AS similarity
            FROM knowledge
            WHERE embedding IS NOT NULL
            ORDER BY embedding <=> CAST(#{embedding} AS vector)
            LIMIT #{topN}
            """)
    List<Map<String, Object>> findSimilarIds(@Param("embedding") String embedding, @Param("topN") int topN);

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

    @Select("""
            SELECT * FROM knowledge
            WHERE content ILIKE CONCAT('%', #{query}, '%')
               OR title ILIKE CONCAT('%', #{query}, '%')
               OR ai_title ILIKE CONCAT('%', #{query}, '%')
            ORDER BY created_at DESC
            LIMIT #{limit}
            """)
    List<Knowledge> keywordSearch(@Param("query") String query, @Param("limit") int limit);

    @Select("""
            SELECT * FROM knowledge
            WHERE (tags IS NOT NULL AND tags @> CAST(#{tagJson} AS jsonb))
               OR (user_tags IS NOT NULL AND user_tags @> CAST(#{tagJson} AS jsonb))
            ORDER BY created_at DESC
            LIMIT #{limit}
            """)
    List<Knowledge> searchByTag(@Param("tagJson") String tagJson, @Param("limit") int limit);

    @Select("SELECT * FROM knowledge WHERE created_at >= #{start} AND created_at < #{end} ORDER BY created_at DESC")
    List<Knowledge> findByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT id FROM knowledge WHERE embedding IS NOT NULL LIMIT 1")
    Long findFirstWithEmbedding();

    @Select("""
            SELECT * FROM knowledge
            WHERE LOWER(title) LIKE LOWER(CONCAT('%', #{title}, '%'))
               OR LOWER(ai_title) LIKE LOWER(CONCAT('%', #{title}, '%'))
               OR LOWER(content) LIKE LOWER(CONCAT('%', #{content}, '%'))
            LIMIT 20
            """)
    List<Knowledge> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(@Param("title") String title, @Param("content") String content);

    @Select("""
            SELECT COUNT(*) FROM knowledge WHERE embedding IS NULL OR embedding = ''
            """)
    int countMissingEmbeddings();

    @Select("SELECT COUNT(*) FROM knowledge WHERE auto_process_status = #{status}")
    int countByAutoProcessStatus(@Param("status") String status);

    @Select("SELECT * FROM knowledge WHERE auto_process_status = #{status} ORDER BY created_at ASC LIMIT #{limit}")
    List<Knowledge> findByAutoProcessStatus(@Param("status") String status, @Param("limit") int limit);

    @Select("SELECT unnest(tags_arr) AS name, COUNT(*) AS count FROM (" +
            "SELECT ARRAY(SELECT jsonb_array_elements_text(tags::jsonb)) AS tags_arr FROM knowledge WHERE tags IS NOT NULL AND tags != '[]' " +
            "UNION ALL " +
            "SELECT ARRAY(SELECT jsonb_array_elements_text(user_tags::jsonb)) AS tags_arr FROM knowledge WHERE user_tags IS NOT NULL AND user_tags != '[]'" +
            ") t GROUP BY name ORDER BY count DESC LIMIT 50")
    List<Map<String, Object>> aggregateTags();
}