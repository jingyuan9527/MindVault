package com.mindvault.operationlog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

import com.mindvault.common.handler.JsonbStringTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 操作审计日志实体。
 * <p>
 * 通过 AOP (@OperationLog 注解 + OperationLogAspect) 自动记录用户的关键操作，
 * 包含操作模块、类型、实体 ID、前后快照、操作人、IP、耗时和结果。
 * 用于审计追踪和管理员查看操作历史。
 * </p>
 * <p>
 * 关键设计:
 * <ul>
 *   <li>detail、beforeSnapshot、afterSnapshot 字段使用 JSONB 类型存储，通过 JsonbStringTypeHandler 处理</li>
 *   <li>beforeSnapshot 和 afterSnapshot 默认不返回（select = false），需通过 selectDetailById 方法单独查询</li>
 *   <li>entityId 支持 Long 和 String 两种 setter 重载，兼容不同模块的 ID 类型</li>
 * </ul>
 * </p>
 * <p>表: operation_log</p>
 */
@TableName("operation_log")
@Data
@Schema(description = "操作日志实体")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键 ID")
    private Long id;

    @TableField("module")
    @Schema(description = "模块")
    private String module;

    @TableField("action")
    @Schema(description = "操作")
    private String action;

    @TableField("action_type")
    @Schema(description = "操作类型: CREATE/UPDATE/DELETE/OTHER")
    private String actionType;

    @TableField(value = "entity_id")
    @Schema(description = "实体 ID")
    private String entityId;

    @TableField("summary")
    @Schema(description = "摘要")
    private String summary;

    @TableField(value = "detail", typeHandler = JsonbStringTypeHandler.class)
    @Schema(description = "详情（字段变更 JSON）")
    private String detail;

    @TableField(value = "before_snapshot", typeHandler = JsonbStringTypeHandler.class, select = false)
    @Schema(description = "操作前快照")
    private String beforeSnapshot;

    @TableField(value = "after_snapshot", typeHandler = JsonbStringTypeHandler.class, select = false)
    @Schema(description = "操作后快照")
    private String afterSnapshot;

    @TableField("operator")
    @Schema(description = "操作人")
    private String operator;

    @TableField("operator_id")
    @Schema(description = "操作人 ID")
    private Long operatorId;

    @TableField("ip_address")
    @Schema(description = "请求 IP")
    private String ipAddress;

    @TableField("result")
    @Schema(description = "结果: SUCCESS/FAILURE")
    private String result = "SUCCESS";

    @TableField("error_message")
    @Schema(description = "错误信息")
    private String errorMessage;

    @TableField("duration_ms")
    @Schema(description = "耗时(毫秒)")
    private Long durationMs = 0L;

    @TableField("remark")
    @Schema(description = "备注（如 API Token 名称）")
    private String remark;

    @TableField("created_at")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId != null ? String.valueOf(entityId) : null; }
}