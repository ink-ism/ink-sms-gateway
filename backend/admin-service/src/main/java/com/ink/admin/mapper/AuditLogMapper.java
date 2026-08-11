package com.ink.admin.mapper;

import com.ink.admin.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 审计日志 Mapper
 */
@Mapper
public interface AuditLogMapper {

    int insert(AuditLog auditLog);

    List<AuditLog> findByPage(@Param("module") String module,
                              @Param("action") String action,
                              @Param("keyword") String keyword,
                              @Param("offset") int offset,
                              @Param("limit") int limit);

    int count(@Param("module") String module,
              @Param("action") String action,
              @Param("keyword") String keyword);
}
