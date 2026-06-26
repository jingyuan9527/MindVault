package com.mindvault.dailyreview;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindvault.dailyreview.entity.DailyReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 每日回顾报告 Mapper 接口。
 * <p>
 * 提供按日期查询和获取最近 N 条报告的数据库操作。
 * 继承 MyBatis-Plus BaseMapper 获得基础 CRUD 能力。
 * </p>
 */
@Mapper
public interface DailyReviewMapper extends BaseMapper<DailyReview> {

    /**
     * 按报告日期查询每日回顾。
     * @param reportDate 报告日期
     * @return 该日期的回顾报告，不存在则返回空
     */
    @Select("SELECT * FROM daily_review WHERE report_date = #{reportDate} LIMIT 1")
    Optional<DailyReview> findByReportDate(@Param("reportDate") LocalDate reportDate);

    /**
     * 获取最近 N 条每日回顾报告，按日期降序排列。
     * @param limit 返回条数上限
     * @return 最近 N 条报告列表
     */
    @Select("SELECT * FROM daily_review ORDER BY report_date DESC LIMIT #{limit}")
    List<DailyReview> findTopByOrderByReportDateDesc(@Param("limit") int limit);
}