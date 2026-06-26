package com.mindvault.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解
 *
 * 标注在 Controller 或 Service 方法上，由 OperationLogAspect 切面自动记录操作日志。
 * 支持安装数快照（操作前/操作后）和调用时长统计。
 *
 * 字段说明：
 * - module:    所属模块名，如 "knowledge" / "chat" / "auth"
 * - action:    操作名称，如 "创建知识" / "删除知识"
 * - actionType: 操作类型分类（CREATE / UPDATE / DELETE / READ / OTHER），影响快照记录策略
 * - entityType: 操作实体类，用于从 SnapshotProvider 中查找对应 Mapper 获取快照
 * - summary:   操作摘要，支持 SpEL 表达式（预留）
 * - entityIdExpr: 实体 ID 的 SpEL 表达式（预留）
 * - recordSnapshot: 是否记录数据快照
 * - recordArgs: 是否记录方法参数
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {
    String module() default "";
    String action() default "";
    String actionType() default "OTHER";
    Class<?> entityType() default Void.class;
    String summary() default "";
    String entityIdExpr() default "";
    boolean recordSnapshot() default false;
    boolean recordArgs() default false;
}