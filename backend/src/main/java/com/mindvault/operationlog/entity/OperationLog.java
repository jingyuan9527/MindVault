package com.mindvault.operationlog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@TableName("operation_log")
@Data
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("module")
    private String module;

    @TableField("action")
    private String action;

    @TableField("entity_id")
    private Long entityId;

    @TableField("summary")
    private String summary;

    @TableField("detail")
    private String detail;

    @TableField("operator")
    private String operator = "system";

    @TableField("created_at")
    private LocalDateTime createdAt;
}