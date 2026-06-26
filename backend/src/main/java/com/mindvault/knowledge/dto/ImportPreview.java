package com.mindvault.knowledge.dto;

import java.util.List;

/**
 * 导入预览结果
 *
 * 解析导入 JSON 后返回：
 * - totalCount:    总条目数
 * - newCount:      无冲突（可新增）条数
 * - conflictCount: 有冲突条数
 * - conflicts:     冲突详情列表
 *
 * ConflictItem 记录每条冲突的索引、导入标题、已有标题
 */
public record ImportPreview(
    int totalCount,
    int newCount,
    int conflictCount,
    List<ConflictItem> conflicts
) {
    public record ConflictItem(
        int index,
        String title,
        String existingTitle
    ) {}
}