package com.ink.admin.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 操作审计日志实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class AuditLog {

    private Long id;

    /** 管理员ID */
    private Long adminId;

    /** 管理员用户名 */
    private String username;

    /** 模块：CHANNEL/SP/ADMIN/BLACKLIST/SIGNATURE/TEMPLATE/SENSITIVE */
    private String module;

    /** 操作：CREATE/UPDATE/DELETE/ENABLE/DISABLE/RECHARGE/APPROVE/REJECT */
    private String action;

    /** 操作对象标识 */
    private String target;

    /** 参数摘要 */
    private String detail;

    /** 操作IP */
    private String ip;

    /** 操作时间 */
    private LocalDateTime createTime;
}
