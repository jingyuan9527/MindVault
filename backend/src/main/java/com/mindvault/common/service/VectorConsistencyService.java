package com.mindvault.common.service;

import com.mindvault.knowledge.KnowledgeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class VectorConsistencyService {

    private static final Logger log = LoggerFactory.getLogger(VectorConsistencyService.class);

    private final KnowledgeMapper knowledgeMapper;

    public VectorConsistencyService(KnowledgeMapper knowledgeMapper) {
        this.knowledgeMapper = knowledgeMapper;
    }

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