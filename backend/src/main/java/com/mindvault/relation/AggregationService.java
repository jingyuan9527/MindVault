package com.mindvault.relation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.auto.AutoProcessLogMapper;
import com.mindvault.auto.entity.AutoProcessLog;
import com.mindvault.knowledge.KnowledgeMapper;
import com.mindvault.knowledge.KnowledgeService;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.systemconfig.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 聚合统计服务（R3 阶段）
 *
 * 对完成 R2 处理（RELATION_DONE 状态）的知识条目执行最终处理：
 * 1. 标记状态为 COMPLETED（自动处理流水线结束）
 * 2. 重建标签云（统计所有标签使用频率）
 *
 * 调度方式：由 AutoProcessScheduler 每 30 分钟触发。
 */
@Service
public class AggregationService {

    private static final Logger log = LoggerFactory.getLogger(AggregationService.class);

    private final KnowledgeMapper knowledgeMapper;
    private final KnowledgeService knowledgeService;
    private final AutoProcessLogMapper logMapper;
    private final SystemConfigService config;
    private final ObjectMapper objectMapper;

    public AggregationService(KnowledgeMapper knowledgeMapper,
                              KnowledgeService knowledgeService,
                              AutoProcessLogMapper logMapper,
                              SystemConfigService config) {
        this.knowledgeMapper = knowledgeMapper;
        this.knowledgeService = knowledgeService;
        this.logMapper = logMapper;
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    /** R3 入口：批量完成 RELATION_DONE → COMPLETED，重建标签云 */
    public void processRound3() {
        int batchSize = config.getInt("threshold.aggregation.batch-size", 50);
        List<Knowledge> pending = knowledgeMapper.findByAutoProcessStatus("RELATION_DONE", batchSize);
        if (pending.isEmpty()) return;
        log.info("R3 聚合分析: 待处理 {} 条", pending.size());

        for (Knowledge k : pending) {
            try {
                knowledgeService.updateAutoProcessStatus(k.getId(), "COMPLETED");
                saveLog(k.getId(), "R3_AGGREGATION", "SUCCESS", null);
            } catch (Exception e) {
                log.warn("R3 聚合失败: knowledgeId={}, error={}", k.getId(), e.getMessage());
                saveLog(k.getId(), "R3_AGGREGATION", "FAILED", e.getMessage());
            }
        }

        rebuildTagCloud();
        log.info("R3 聚合分析完成");
    }

    /** 重建标签云：聚合所有知识的 ai_tags + user_tags，统计使用频率 */
    public void rebuildTagCloud() {
        try {
            List<Knowledge> all = knowledgeMapper.selectList(null);
            Map<String, Long> tagCount = new HashMap<>();

            for (Knowledge k : all) {
                countTags(k.getTags(), tagCount);
                countTags(k.getUserTags(), tagCount);
            }

            int topN = config.getInt("threshold.aggregation.tag-cloud-top-n", 50);
            List<Map.Entry<String, Long>> sorted = tagCount.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(topN)
                    .toList();

            log.info("标签云更新完成: 共 {} 个标签", sorted.size());
        } catch (Exception e) {
            log.warn("标签云更新失败: {}", e.getMessage());
        }
    }

    /** 解析并累计单条知识的标签到统计 Map */
    private void countTags(String tagsJson, Map<String, Long> tagCount) {
        if (tagsJson == null || tagsJson.equals("[]")) return;
        try {
            List<String> tags = objectMapper.readValue(tagsJson, new TypeReference<List<String>>() {});
            for (String tag : tags) {
                tagCount.merge(tag, 1L, Long::sum);
            }
        } catch (Exception e) {
            log.warn("解析标签失败: {}", e.getMessage());
        }
    }

    /** 记录 R3 处理日志 */
    private void saveLog(Long knowledgeId, String round, String status, String errorMessage) {
        try {
            AutoProcessLog l = new AutoProcessLog();
            l.setKnowledgeId(knowledgeId);
            l.setRound(round);
            l.setStatus(status);
            l.setErrorMessage(errorMessage);
            l.setStartedAt(LocalDateTime.now());
            l.setCompletedAt(LocalDateTime.now());
            l.setCreatedAt(LocalDateTime.now());
            logMapper.insert(l);
        } catch (Exception e) {
            log.warn("保存处理日志失败: {}", e.getMessage());
        }
    }
}
