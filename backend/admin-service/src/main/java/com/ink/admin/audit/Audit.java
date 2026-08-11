package com.ink.admin.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计日志注解
 * 标注在 Controller 写操作方法上，由 AuditAspect 自动记录操作日志
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audit {

    /** 模块：CHANNEL/SP/ADMIN/BLACKLIST/SIGNATURE/TEMPLATE/SENSITIVE */
    String module();

    /** 操作：CREATE/UPDATE/DELETE/ENABLE/DISABLE/RECHARGE/APPROVE/REJECT */
    String action();
}
