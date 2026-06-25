package com.mindvault.operationlog;

import com.mindvault.common.dto.PageResult;
import com.mindvault.operationlog.entity.OperationLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OperationLogService {

    private static final Logger opLog = LoggerFactory.getLogger("OPERATION_LOG");

    private final OperationLogMapper mapper;

    public OperationLogService(OperationLogMapper mapper) {
        this.mapper = mapper;
    }

    public void log(OperationLog entry) {
        if (entry.getCreatedAt() == null) {
            entry.setCreatedAt(java.time.LocalDateTime.now());
        }
        mapper.insert(entry);
        opLog.info("[{}][{}] entityId={} | {} | {}ms | {}",
                entry.getModule(), entry.getAction(), entry.getEntityId(),
                entry.getSummary(), entry.getDurationMs(), entry.getResult());
    }

    public void log(String module, String action, Long entityId, String summary) {
        OperationLog entry = new OperationLog();
        entry.setModule(module);
        entry.setAction(action);
        entry.setEntityId(entityId);
        entry.setSummary(summary);
        entry.setCreatedAt(java.time.LocalDateTime.now());
        log(entry);
    }

    public PageResult<OperationLog> listPage(String module, int page, int size) {
        int offset = page * size;
        List<OperationLog> records;
        long total;

        if (module != null && !module.isEmpty()) {
            records = mapper.findByModuleOrderByCreatedAtDesc(module);
            total = mapper.countByModule(module);
        } else {
            records = mapper.listAll();
            total = mapper.countAll();
        }

        int fromIndex = Math.min(offset, records.size());
        int toIndex = Math.min(offset + size, records.size());
        List<OperationLog> paged = records.subList(fromIndex, toIndex);

        int totalPages = (int) Math.ceil((double) total / size);
        return new PageResult<>(paged, total, page, size, totalPages);
    }

    public List<OperationLog> listByModule(String module) {
        return mapper.findByModuleOrderByCreatedAtDesc(module);
    }

    public List<OperationLog> listAll() {
        return mapper.listAll();
    }

    public OperationLog getDetail(Long id) {
        return mapper.selectDetailById(id);
    }
}