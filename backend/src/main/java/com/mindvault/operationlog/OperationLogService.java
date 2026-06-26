package com.mindvault.operationlog;

import com.mindvault.common.dto.PageResult;
import com.mindvault.operationlog.entity.OperationLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 操作日志服务。
 * <p>
 * 核心职责: 记录用户操作日志（通过 @OperationLog AOP 或手动调用），
 * 提供分页查询、按模块筛选、详情查询功能。
 * </p>
 * <p>
 * 关键设计:
 * <ul>
 *   <li>日志记录同时写入 DB 和日志文件（OPERATION_LOG logger），便于排查</li>
 *   <li>分页查询在内存中进行（查全量后截取），因为操作日志量级可控</li>
 *   <li>提供两个 log 重载方法: 直接传入 OperationLog 对象，或通过模块/操作/实体ID/摘要快捷记录</li>
 * </ul>
 * </p>
 * <p>依赖: OperationLogMapper</p>
 */
@Service
public class OperationLogService {

    private static final Logger opLog = LoggerFactory.getLogger("OPERATION_LOG");

    private final OperationLogMapper mapper;

    public OperationLogService(OperationLogMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 记录一条操作日志。
     * @param entry 操作日志实体（未设置创建时间则自动填充当前时间）
     */
    public void log(OperationLog entry) {
        if (entry.getCreatedAt() == null) {
            entry.setCreatedAt(java.time.LocalDateTime.now());
        }
        mapper.insert(entry);
        opLog.info("[{}][{}] entityId={} | {} | {}ms | {}",
                entry.getModule(), entry.getAction(), entry.getEntityId(),
                entry.getSummary(), entry.getDurationMs(), entry.getResult());
    }

    /**
     * 快捷方法: 按模块/操作/实体ID/摘要记录一条操作日志。
     * @param module   模块名称
     * @param action   操作名称
     * @param entityId 操作的实体 ID（可为 null）
     * @param summary  操作摘要
     */
    public void log(String module, String action, Long entityId, String summary) {
        OperationLog entry = new OperationLog();
        entry.setModule(module);
        entry.setAction(action);
        entry.setEntityId(entityId);
        entry.setSummary(summary);
        entry.setCreatedAt(java.time.LocalDateTime.now());
        log(entry);
    }

    /**
     * 分页查询操作日志。
     * 支持按模块筛选；分页在内存中进行（查全量后截取）。
     * @param module 模块名称（可选，为空则查全部）
     * @param page   页码（从 0 开始）
     * @param size   每页条数
     * @return 分页结果
     */
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

    /**
     * 按模块查询日志列表。
     * @param module 模块名称
     * @return 日志列表（不含快照字段）
     */
    public List<OperationLog> listByModule(String module) {
        return mapper.findByModuleOrderByCreatedAtDesc(module);
    }

    /**
     * 查询所有日志。
     * @return 全量日志列表（不含快照字段）
     */
    public List<OperationLog> listAll() {
        return mapper.listAll();
    }

    /**
     * 获取单条日志详情（含 detail 字段）。
     * @param id 日志主键
     * @return 日志详情
     */
    public OperationLog getDetail(Long id) {
        return mapper.selectDetailById(id);
    }
}