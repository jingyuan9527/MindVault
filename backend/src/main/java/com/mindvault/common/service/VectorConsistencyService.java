package com.mindvault.common.service;

import com.mindvault.knowledge.mapper.KnowledgeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 向量一致性检查服务
 *
 * 定时任务，每天凌晨 3:45 检查知识库中是否存在缺少嵌入向量的条目。
 * 如果发现缺失，记录警告日志；如果全部正常，记录通过日志。
 * 此检查确保语义搜索的向量索引始终完整可用。
 *
 * 依赖：KnowledgeMapper（直接通过 Mapper 的 countMissingEmbeddings 方法查询）
 */
@Service
public class VectorConsistencyService {

    private static final Logger log = LoggerFactory.getLogger(VectorConsistencyService.class);

    private final KnowledgeMapper knowledgeMapper;

    public VectorConsistencyService(KnowledgeMapper knowledgeMapper) {
        this.knowledgeMapper = knowledgeMapper;
    }

    /** 定时执行向量一致性检查（每天 03:45），统计缺失嵌入向量的知识条目数 */
    @Scheduled(cron = "0 45 3 * * ?")
    public void scheduledVectorConsistencyCheck() {
        log.info("开始执行定时向量一致性检查...");
        try {
            long total = knowledgeMapper.selectCount(null);
            int missingEmbeddings = knowledgeMapper.countMissingEmbeddings();
            if (missingEmbeddings > 0) {
                log.warn("向量一致性检查: 共 {} 条知识, {} 条缺少嵌入向量", total, missingEmbeddings);
            } else {
                log.info("向量一致性检查通过: 共 {} 条知识, 全部拥有嵌入向量", total);
            }
        } catch (Exception e) {
            log.warn("向量一致性检查失败: {}", e.getMessage());
        }
    }
}