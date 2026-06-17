package com.mindvault.knowledge;

import com.mindvault.knowledge.entity.Knowledge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 知识库 Repository
 *
 * 除了标准 CRUD，还包含 PGVector 的余弦相似度查询。
 * Agent 的 searchKnowledge Tool 会调用 searchSimilar() 方法。
 *
 * 向量查询使用 PostgreSQL 的 <=> 操作符（余弦距离），
 * 通过 HNSW 索引加速近似最近邻搜索。
 */
@Repository
public interface KnowledgeRepository extends JpaRepository<Knowledge, Long> {

    /**
     * 向量相似度搜索 — 返回 (id, similarity) 元组
     *
     * 使用 PGVector 的余弦距离操作符 (<=>)，
     * CAST(:embedding AS vector) 将字符串参数转换为 vector 类型
     */
    @Query(value = """
            SELECT id, 1 - (embedding <=> CAST(:embedding AS vector)) AS similarity
            FROM knowledge
            WHERE embedding IS NOT NULL
            ORDER BY embedding <=> CAST(:embedding AS vector)
            LIMIT :topN
            """, nativeQuery = true)
    List<Object[]> findSimilarIds(@Param("embedding") String embedding, @Param("topN") int topN);

    /**
     * 全文检索（v0.1 基础版）
     * v0.2 将实现关键字 + 向量的混合检索
     */
    @Query(value = """
            SELECT * FROM knowledge
            WHERE content ILIKE CONCAT('%', :query, '%')
               OR title ILIKE CONCAT('%', :query, '%')
            ORDER BY created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Knowledge> keywordSearch(@Param("query") String query, @Param("limit") int limit);

    @Query(value = """
            SELECT * FROM knowledge
            WHERE tags IS NOT NULL
              AND tags @> CAST(:tagJson AS jsonb)
            ORDER BY created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Knowledge> searchByTag(@Param("tagJson") String tagJson, @Param("limit") int limit);

    /** 按 content_type 过滤 */
    List<Knowledge> findByContentTypeOrderByCreatedAtDesc(String contentType);
}