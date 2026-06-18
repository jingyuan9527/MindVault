package com.mindvault.dailyreview;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindvault.dailyreview.entity.DailyReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Mapper
public interface DailyReviewMapper extends BaseMapper<DailyReview> {

    @Select("SELECT * FROM daily_review WHERE report_date = #{reportDate} LIMIT 1")
    Optional<DailyReview> findByReportDate(@Param("reportDate") LocalDate reportDate);

    @Select("SELECT * FROM daily_review ORDER BY report_date DESC LIMIT #{limit}")
    List<DailyReview> findTopByOrderByReportDateDesc(@Param("limit") int limit);
}