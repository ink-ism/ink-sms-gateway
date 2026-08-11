package com.ink.admin.controller;

import com.ink.admin.audit.Audit;
import com.ink.admin.entity.SensitiveWord;
import com.ink.admin.service.SensitiveWordService;
import com.ink.common.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 敏感词管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/sensitives")
@Tag(name = "敏感词管理", description = "敏感词CRUD，变更后通知api服务刷新")
@RequiredArgsConstructor
public class SensitiveWordController {

    private final SensitiveWordService sensitiveWordService;

    @Operation(summary = "获取敏感词列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> getList(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Integer status) {
        Map<String, Object> data = new HashMap<>();
        data.put("list", sensitiveWordService.getList(keyword, status, page, size));
        data.put("total", sensitiveWordService.getCount(keyword, status));
        data.put("page", page);
        data.put("size", size);
        return Result.success(data);
    }

    @Operation(summary = "创建敏感词")
    @PostMapping
    @Audit(module = "SENSITIVE", action = "CREATE")
    public Result<String> create(@Valid @RequestBody SensitiveWord word) {
        sensitiveWordService.create(word);
        return Result.success("敏感词创建成功");
    }

    @Operation(summary = "更新敏感词")
    @PutMapping("/{id}")
    @Audit(module = "SENSITIVE", action = "UPDATE")
    public Result<String> update(@PathVariable Long id, @Valid @RequestBody SensitiveWord word) {
        word.setId(id);
        sensitiveWordService.update(word);
        return Result.success("敏感词更新成功");
    }

    @Operation(summary = "启用敏感词")
    @PutMapping("/{id}/enable")
    @Audit(module = "SENSITIVE", action = "ENABLE")
    public Result<String> enable(@PathVariable Long id) {
        sensitiveWordService.enable(id);
        return Result.success("敏感词已启用");
    }

    @Operation(summary = "禁用敏感词")
    @PutMapping("/{id}/disable")
    @Audit(module = "SENSITIVE", action = "DISABLE")
    public Result<String> disable(@PathVariable Long id) {
        sensitiveWordService.disable(id);
        return Result.success("敏感词已禁用");
    }

    @Operation(summary = "删除敏感词")
    @DeleteMapping("/{id}")
    @Audit(module = "SENSITIVE", action = "DELETE")
    public Result<String> delete(@PathVariable Long id) {
        sensitiveWordService.delete(id);
        return Result.success("敏感词删除成功");
    }
}
