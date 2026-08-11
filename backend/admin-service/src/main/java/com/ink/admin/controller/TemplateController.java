package com.ink.admin.controller;

import com.ink.admin.audit.Audit;
import com.ink.admin.entity.Template;
import com.ink.admin.service.TemplateService;
import com.ink.common.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 短信模板管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/templates")
@Tag(name = "模板管理", description = "短信模板CRUD与审核")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @Operation(summary = "获取模板列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> getList(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Integer status) {
        Map<String, Object> data = new HashMap<>();
        data.put("list", templateService.getList(keyword, status, page, size));
        data.put("total", templateService.getCount(keyword, status));
        data.put("page", page);
        data.put("size", size);
        return Result.success(data);
    }

    @Operation(summary = "获取模板详情")
    @GetMapping("/{id}")
    public Result<Template> get(@PathVariable Long id) {
        return Result.success(templateService.getById(id));
    }

    @Operation(summary = "创建模板")
    @PostMapping
    @Audit(module = "TEMPLATE", action = "CREATE")
    public Result<String> create(@Valid @RequestBody Template template) {
        templateService.create(template);
        return Result.success("模板创建成功，待审核");
    }

    @Operation(summary = "更新模板")
    @PutMapping("/{id}")
    @Audit(module = "TEMPLATE", action = "UPDATE")
    public Result<String> update(@PathVariable Long id, @Valid @RequestBody Template template) {
        template.setId(id);
        templateService.update(template);
        return Result.success("模板更新成功");
    }

    @Operation(summary = "审核通过")
    @PutMapping("/{id}/approve")
    @Audit(module = "TEMPLATE", action = "APPROVE")
    public Result<String> approve(@PathVariable Long id, @RequestBody(required = false) RemarkRequest request) {
        templateService.approve(id, request != null ? request.getRemark() : null);
        return Result.success("模板审核通过");
    }

    @Operation(summary = "审核驳回")
    @PutMapping("/{id}/reject")
    @Audit(module = "TEMPLATE", action = "REJECT")
    public Result<String> reject(@PathVariable Long id, @RequestBody(required = false) RemarkRequest request) {
        templateService.reject(id, request != null ? request.getRemark() : null);
        return Result.success("模板已驳回");
    }

    @Operation(summary = "删除模板")
    @DeleteMapping("/{id}")
    @Audit(module = "TEMPLATE", action = "DELETE")
    public Result<String> delete(@PathVariable Long id) {
        templateService.delete(id);
        return Result.success("模板删除成功");
    }

    @Data
    public static class RemarkRequest {
        /** 审核备注 */
        private String remark;
    }
}
