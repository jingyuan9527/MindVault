package com.mindvault.operationlog.service;

import com.mindvault.common.dto.PageResult;
import com.mindvault.operationlog.entity.OperationLog;

import java.util.List;

public interface OperationLogService {

    void log(OperationLog entry);

    void log(String module, String action, Long entityId, String summary);

    PageResult<OperationLog> listPage(String module, int page, int size);

    List<OperationLog> listByModule(String module);

    List<OperationLog> listAll();

    OperationLog getDetail(Long id);
}