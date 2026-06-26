package com.mindvault.review;

import com.mindvault.knowledge.KnowledgeService;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.operationlog.OperationLogService;
import com.mindvault.review.entity.ReviewSchedule;
import com.mindvault.systemconfig.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 间隔重复复习服务（SM-2 算法实现）。
 * <p>管理知识的复习调度，核心算法基于 SM-2：根据用户质量评分（0-5 分）动态调整简易系数和间隔天数。
 * 评分 < 3 时重置间隔并降低 ease_factor；评分 >= 3 时逐步延长间隔。
 * 首次成功间隔 1 天，第二次成功 6 天，之后间隔 = 当前间隔 × ease_factor。
 * 输入为 knowledgeId + quality 评分，输出为更新后的 ReviewSchedule。
 * 所有阈值参数（ease_factor 上下限、初始间隔、评分范围等）通过 SystemConfigService 动态配置。</p>
 */
@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewScheduleMapper mapper;
    private final KnowledgeService knowledgeService;
    private final OperationLogService operationLogService;
    private final SystemConfigService config;

    public ReviewService(ReviewScheduleMapper mapper,
                         @Lazy KnowledgeService knowledgeService,
                         OperationLogService operationLogService,
                         SystemConfigService config) {
        this.mapper = mapper;
        this.knowledgeService = knowledgeService;
        this.operationLogService = operationLogService;
        this.config = config;
    }

    /**
     * 为指定知识安排复习计划。
     * 若已有计划则直接返回，否则新建计划（初始间隔从配置读取）。
     * @param knowledgeId 知识 ID
     * @return 复习计划对象
     */
    @Transactional
    public ReviewSchedule scheduleReview(Long knowledgeId) {
        Optional<ReviewSchedule> existing = mapper.findByKnowledgeId(knowledgeId);
        if (existing.isPresent()) {
            return existing.get();
        }
        ReviewSchedule schedule = new ReviewSchedule();
        LocalDateTime now = LocalDateTime.now();
        schedule.setKnowledgeId(knowledgeId);
        schedule.setNextReviewAt(now.plusDays(config.getInt("threshold.review.initial-interval-days", 1)));
        schedule.setCreatedAt(now);
        schedule.setUpdatedAt(now);
        mapper.insert(schedule);
        log.info("安排复习: knowledgeId={}, nextReviewAt={}", knowledgeId, schedule.getNextReviewAt());
        return schedule;
    }

    /**
     * 执行复习并提交质量评分。
     * SM-2 算法核心逻辑：根据 quality 决定间隔调整策略。
     * 质量 0-2 视为失败（重置间隔、降低 ease_factor），质量 3-5 视为成功（递增间隔）。
     * @param knowledgeId 知识 ID
     * @param quality 质量评分（0-5，会自动钳制到有效范围）
     * @return 更新后的复习计划
     */
    @Transactional
    public ReviewSchedule performReview(Long knowledgeId, int quality) {
        int qualityMin = config.getInt("threshold.review.quality-min", 0);
        int qualityMax = config.getInt("threshold.review.quality-max", 5);
        quality = Math.max(qualityMin, Math.min(qualityMax, quality));
        ReviewSchedule schedule = mapper.findByKnowledgeId(knowledgeId)
                .orElseGet(() -> {
                    ReviewSchedule s = new ReviewSchedule();
                    s.setKnowledgeId(knowledgeId);
                    return s;
                });

        BigDecimal easeFactor = schedule.getEaseFactor();
        int interval = schedule.getIntervalDays();
        int reviewCount = schedule.getReviewCount();

        if (quality < 3) {
            interval = config.getInt("threshold.review.failed-interval-days", 1);
            easeFactor = easeFactor.subtract(new BigDecimal(String.valueOf(config.getDouble("threshold.review.ease-factor-penalty", 0.20))));
        } else {
            if (reviewCount == 0) {
                interval = config.getInt("threshold.review.first-success-interval", 1);
            } else if (reviewCount == 1) {
                interval = config.getInt("threshold.review.second-success-interval", 6);
            } else {
                interval = new BigDecimal(interval)
                        .multiply(easeFactor)
                        .setScale(0, RoundingMode.HALF_UP)
                        .intValue();
            }
            BigDecimal qualityAdjust = new BigDecimal(quality - 3)
                    .multiply(new BigDecimal(String.valueOf(config.getDouble("threshold.review.ease-factor-adjustment", 0.10))));
            easeFactor = easeFactor.add(qualityAdjust);
        }

        BigDecimal minEase = new BigDecimal(String.valueOf(config.getDouble("threshold.review.min-ease-factor", 1.30)));
        if (easeFactor.compareTo(minEase) < 0) {
            easeFactor = minEase;
        }

        schedule.setEaseFactor(easeFactor.setScale(2, RoundingMode.HALF_UP));
        int minInterval = config.getInt("threshold.review.failed-interval-days", 1);
        schedule.setIntervalDays(Math.max(minInterval, interval));
        schedule.setReviewCount(reviewCount + 1);
        schedule.setLastReviewAt(LocalDateTime.now());
        schedule.setNextReviewAt(LocalDateTime.now().plusDays(Math.max(minInterval, interval)));

        LocalDateTime now = LocalDateTime.now();
        if (schedule.getId() == null) {
            schedule.setCreatedAt(now);
            schedule.setUpdatedAt(now);
            mapper.insert(schedule);
        } else {
            schedule.setUpdatedAt(now);
            mapper.updateById(schedule);
        }

        log.info("执行复习: knowledgeId={}, quality={}, nextInterval={}d, nextAt={}",
                knowledgeId, quality, schedule.getIntervalDays(), schedule.getNextReviewAt());
        operationLogService.log("REVIEW", "PERFORM", knowledgeId,
                "复习知识，质量=" + quality + "，下次复习=" + schedule.getNextReviewAt());
        return schedule;
    }

    /**
     * 获取到期待复习的知识列表。
     * 查询到期计划后关联查询 Knowledge 对象，组装为包含标题、内容、标签等信息的 Map 列表。
     * @param limit 最大返回条数
     * @return 复习项列表，每项含知识信息和复习进度
     */
    public List<Map<String, Object>> getDueReviews(int limit) {
        List<ReviewSchedule> dueSchedules = mapper.findDueReviews(LocalDateTime.now());
        if (dueSchedules.size() > limit) {
            dueSchedules = dueSchedules.subList(0, limit);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (ReviewSchedule s : dueSchedules) {
            try {
                Knowledge k = knowledgeService.getById(s.getKnowledgeId());
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", s.getId());
                item.put("knowledgeId", k.getId());
                item.put("title", k.getTitle());
                item.put("content", k.getContent());
                item.put("summary", k.getSummary());
                item.put("tags", k.getTags());
                item.put("reviewCount", s.getReviewCount());
                item.put("intervalDays", s.getIntervalDays());
                item.put("easeFactor", s.getEaseFactor());
                item.put("nextReviewAt", s.getNextReviewAt());
                result.add(item);
            } catch (Exception e) {
                log.warn("复习知识不存在，跳过: knowledgeId={}", s.getKnowledgeId());
            }
        }
        return result;
    }

    /**
     * 获取当前待复习的知识总数。
     * @return 到期未复习数量
     */
    public long getDueReviewCount() {
        return mapper.countDueReviews(LocalDateTime.now());
    }
}