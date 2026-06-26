package com.mindvault.auto.r2;

import com.mindvault.knowledge.entity.Knowledge;

/**
 * 知识关联发现服务（R2 阶段）
 *
 * 对完成 R1 处理（TITLE_TAG_DONE 状态）的知识条目，通过三种策略发现关联：
 * 1. 语义相似度（VECTOR）— 基于 pgvector 余弦距离
 * 2. 标签重叠（TAG）— 共享标签越多，关联分越高
 * 3. LLM 分析（LLM）— 将候选列表提交 LLM 判断关联类型和原因
 */
public interface RelationService {

    /** R2 入口：分批处理 TITLE_TAG_DONE 状态的知识，发现关联后更新状态为 RELATION_DONE */
    void processRound2();

    /** 对单条知识执行三种关联发现策略 */
    void processKnowledgeRelations(Knowledge k);
}