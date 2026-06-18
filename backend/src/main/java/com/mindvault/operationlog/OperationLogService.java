package com.mindvault.operationlog;

import com.mindvault.operationlog.entity.OperationLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OperationLogService {

    private static final Logger opLog = LoggerFactory.getLogger("OPERATION_LOG");

    private final OperationLogMapper mapper;

    public OperationLogService(OperationLogMapper mapper) {
        this.mapper = mapper;
    }

    public void log(String module, String action, Long entityId, String summary) {
        OperationLog log = new OperationLog();
        log.setModule(module);
        log.setAction(action);
        log.setEntityId(entityId);
        log.setSummary(summary);
        log.setCreatedAt(LocalDateTime.now());

        mapper.insert(log);

        opLog.info("[{}][{}] entityId={} | {}", module, action, entityId, summary);
    }

    public List<OperationLog> listByModule(String module) {
        return mapper.findByModuleOrderByCreatedAtDesc(module);
    }

    public List<OperationLog> listAll() {
        return mapper.selectList(null);
    }
}