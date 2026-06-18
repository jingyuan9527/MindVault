package com.mindvault.review;

import com.mindvault.knowledge.KnowledgeService;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.operationlog.OperationLogService;
import com.mindvault.review.entity.ReviewSchedule;
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
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewScheduleRepository repository;
    private final KnowledgeService knowledgeService;
    private final OperationLogService operationLogService;

    public ReviewService(ReviewScheduleRepository repository,
                         @Lazy KnowledgeService knowledgeService,
                         OperationLogService operationLogService) {
        this.repository = repository;
        this.knowledgeService = knowledgeService;
        this.operationLogService = operationLogService;
    }

    @Transactional
    public ReviewSchedule scheduleReview(Long knowledgeId) {
        Optional<ReviewSchedule> existing = repository.findByKnowledgeId(knowledgeId);
        if (existing.isPresent()) {
            return existing.get();
        }
        ReviewSchedule schedule = new ReviewSchedule();
        schedule.setKnowledgeId(knowledgeId);
        schedule.setNextReviewAt(LocalDateTime.now().plusDays(1));
        ReviewSchedule saved = repository.save(schedule);
        log.info("安排复习: knowledgeId={}, nextReviewAt={}", knowledgeId, saved.getNextReviewAt());
        return saved;
    }

    @Transactional
    public ReviewSchedule performReview(Long knowledgeId, int quality) {
        quality = Math.max(0, Math.min(5, quality));
        ReviewSchedule schedule = repository.findByKnowledgeId(knowledgeId)
                .orElseGet(() -> {
                    ReviewSchedule s = new ReviewSchedule();
                    s.setKnowledgeId(knowledgeId);
                    return s;
                });

        BigDecimal easeFactor = schedule.getEaseFactor();
        int interval = schedule.getIntervalDays();
        int reviewCount = schedule.getReviewCount();

        if (quality < 3) {
            interval = 1;
            easeFactor = easeFactor.subtract(new BigDecimal("0.20"));
        } else {
            if (reviewCount == 0) {
                interval = 1;
            } else if (reviewCount == 1) {
                interval = 6;
            } else {
                interval = new BigDecimal(interval)
                        .multiply(easeFactor)
                        .setScale(0, RoundingMode.HALF_UP)
                        .intValue();
            }
            BigDecimal qualityAdjust = new BigDecimal(quality - 3)
                    .multiply(new BigDecimal("0.10"));
            easeFactor = easeFactor.add(qualityAdjust);
        }

        if (easeFactor.compareTo(new BigDecimal("1.30")) < 0) {
            easeFactor = new BigDecimal("1.30");
        }

        schedule.setEaseFactor(easeFactor.setScale(2, RoundingMode.HALF_UP));
        schedule.setIntervalDays(Math.max(1, interval));
        schedule.setReviewCount(reviewCount + 1);
        schedule.setLastReviewAt(LocalDateTime.now());
        schedule.setNextReviewAt(LocalDateTime.now().plusDays(Math.max(1, interval)));

        ReviewSchedule saved = repository.save(schedule);
        log.info("执行复习: knowledgeId={}, quality={}, nextInterval={}d, nextAt={}",
                knowledgeId, quality, saved.getIntervalDays(), saved.getNextReviewAt());
        operationLogService.log("REVIEW", "PERFORM", knowledgeId,
                "复习知识，质量=" + quality + "，下次复习=" + saved.getNextReviewAt());
        return saved;
    }

    public List<Map<String, Object>> getDueReviews(int limit) {
        List<ReviewSchedule> dueSchedules = repository.findDueReviews(LocalDateTime.now());
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

    public long getDueReviewCount() {
        return repository.countDueReviews(LocalDateTime.now());
    }
}