package com.mindvault.common.dto;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 通用分页响应
 *
 * 统一分页查询的返回格式，兼容 MyBatis-Plus IPage。
 * 注意 page 为 0-based（与 MyBatis-Plus 的 1-based 不同，此处减 1 后返回）。
 *
 * 静态工厂方法：
 * - of(IPage mpPage): 从 MyBatis-Plus 分页对象转换
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "通用分页响应")
public class PageResult<T> {
    @Schema(description = "数据列表")
    private List<T> records;

    @Schema(description = "总条数")
    private long total;

    @Schema(description = "当前页码（0-based）")
    private int page;

    @Schema(description = "每页条数")
    private int size;

    @Schema(description = "总页数")
    private int totalPages;

    public static <T> PageResult<T> of(IPage<T> mpPage) {
        return new PageResult<>(
                mpPage.getRecords(),
                mpPage.getTotal(),
                (int) mpPage.getCurrent() - 1,
                (int) mpPage.getSize(),
                (int) mpPage.getPages()
        );
    }
}