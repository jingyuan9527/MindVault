package com.mindvault.knowledge.service.strategy;

import com.mindvault.knowledge.entity.Knowledge;

import java.util.List;

public interface ExportFormatStrategy {
    String getFormat();
    byte[] export(List<Knowledge> items);
    boolean isApplicable(List<Knowledge> items);
}