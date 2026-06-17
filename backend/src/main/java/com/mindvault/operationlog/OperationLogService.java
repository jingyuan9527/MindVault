package com.mindvault.operationlog;

import com.mindvault.operationlog.entity.OperationLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 操作日志服务
 *
 * 所有业务模块调用此服务记录关键操作。
 * 同步写入 DB + 异步写入操作日志文件。
 */
@Service
public class OperationLogService {

    private static final Logger opLog = LoggerFactory.getLogger("OPERATION_LOG");

    private final OperationLogRepository repository;

    public OperationLogService(OperationLogRepository repository) {
        this.repository = repository;
    }

    /**
     * 记录操作日志
     *
     * @param module   模块名 KNOWLEDGE|CHAT|MODEL|SYSTEM
     * @param action   操作类型 ADD|DELETE|SEARCH|EXPORT|TEST 等
     * @param entityId 操作对象 ID（可为 null）
     * @param summary  可读的描述文字
     */
    public void log(String module, String action, Long entityId, String summary) {
        OperationLog log = new OperationLog();
        log.setModule(module);
        log.setAction(action);
        log.setEntityId(entityId);
        log.setSummary(summary);

        repository.save(log);

        // 同时输出到操作日志文件
        opLog.info("[{}][{}] entityId={} | {}", module, action, entityId, summary);
    }

    /** 按模块查询操作日志 */
    public List<OperationLog> listByModule(String module) {
        return repository.findByModuleOrderByCreatedAtDesc(module);
    }

    /** 查询所有操作日志 */
    public List<OperationLog> listAll() {
        return repository.findAll();
    }
}