package com.ink.admin.controller;

import com.ink.admin.entity.AuditLog;
import com.ink.admin.mapper.AuditLogMapper;
import com.ink.common.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审计日志查询控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/audit-logs")
@Tag(name = "审计日志", description = "管理员操作审计日志查询")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogMapper auditLogMapper;

    @Operation(summary = "分页查询审计日志")
    @GetMapping("/list")
    public Result<Map<String, Object>> getList(
            @RequestParam(value = "page", defaultValue = "1") @Parameter(description = "页码") int page,
            @RequestParam(value = "size", defaultValue = "10") @Parameter(description = "每页大小") int size,
            @RequestParam(value = "module", required = false) @Parameter(description = "模块") String module,
            @RequestParam(value = "action", required = false) @Parameter(description = "操作") String action,
            @RequestParam(value = "keyword", required = false) @Parameter(description = "关键字（用户名/对象/详情）") String keyword) {
        List<AuditLog> list = auditLogMapper.findByPage(module, action, keyword, (page - 1) * size, size);
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", auditLogMapper.count(module, action, keyword));
        data.put("page", page);
        data.put("size", size);
        return Result.success(data);
    }
}
