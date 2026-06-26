package com.mindvault.review.service;

import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.knowledge.service.KnowledgeService;
import com.mindvault.operationlog.service.OperationLogService;
import com.mindvault.review.entity.ReviewSchedule;
import com.mindvault.review.mapper.ReviewScheduleMapper;
import com.mindvault.review.config.ReviewProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ReviewServiceImpl implements ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private final ReviewScheduleMapper mapper;
    private final KnowledgeService knowledgeService;
    private final OperationLogService operationLogService;
    private final ReviewProperties reviewProperties;

    public ReviewServiceImpl(ReviewScheduleMapper mapper,
                             @Lazy KnowledgeService knowledgeService,
                             OperationLogService operationLogService,
                             ReviewProperties reviewProperties) {
        this.mapper = mapper;
        this.knowledgeService = knowledgeService;
        this.operationLogService = operationLogService;
        this.reviewProperties = reviewProperties;
    }

    @Transactional
    @Override
    public ReviewSchedule scheduleReview(Long knowledgeId) {
        Optional<ReviewSchedule> existing = mapper.findByKnowledgeId(knowledgeId);
        if (existing.isPresent()) {
            return existing.get();
        }
        ReviewSchedule schedule = new ReviewSchedule();
        LocalDateTime now = LocalDateTime.now();
        schedule.setKnowledgeId(knowledgeId);
        schedule.setNextReviewAt(now.plusDays(reviewProperties.getInitialIntervalDays()));
        schedule.setCreatedAt(now);
        schedule.setUpdatedAt(now);
        mapper.insert(schedule);
        log.info("安排复习: knowledgeId={}, nextReviewAt={}", knowledgeId, schedule.getNextReviewAt());
        return schedule;
    }

    @Transactional
    @Override
    public ReviewSchedule performReview(Long knowledgeId, int quality) {
        int qualityMin = reviewProperties.getQualityMin();
        int qualityMax = reviewProperties.getQualityMax();
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
            interval = reviewProperties.getFailedIntervalDays();
            easeFactor = easeFactor.subtract(new BigDecimal(String.valueOf(reviewProperties.getEaseFactorPenalty())));
        } else {
            if (reviewCount == 0) {
                interval = reviewProperties.getFirstSuccessInterval();
            } else if (reviewCount == 1) {
                interval = reviewProperties.getSecondSuccessInterval();
            } else {
                interval = new BigDecimal(interval)
                        .multiply(easeFactor)
                        .setScale(0, RoundingMode.HALF_UP)
                        .intValue();
            }
            BigDecimal qualityAdjust = new BigDecimal(quality - 3)
                    .multiply(new BigDecimal(String.valueOf(reviewProperties.getEaseFactorAdjustment())));
            easeFactor = easeFactor.add(qualityAdjust);
        }

        BigDecimal minEase = new BigDecimal(String.valueOf(reviewProperties.getMinEaseFactor()));
        if (easeFactor.compareTo(minEase) < 0) {
            easeFactor = minEase;
        }

        schedule.setEaseFactor(easeFactor.setScale(2, RoundingMode.HALF_UP));
        int minInterval = reviewProperties.getFailedIntervalDays();
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

    @Override
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

    @Override
    public long getDueReviewCount() {
        return mapper.countDueReviews(LocalDateTime.now());
    }
}