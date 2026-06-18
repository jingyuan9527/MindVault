package com.mindvault.knowledge.dto;

import java.util.List;

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