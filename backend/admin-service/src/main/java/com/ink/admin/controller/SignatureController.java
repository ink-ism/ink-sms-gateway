package com.ink.admin.controller;

import com.ink.admin.audit.Audit;
import com.ink.admin.entity.Signature;
import com.ink.admin.service.SignatureService;
import com.ink.common.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 短信签名管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/signatures")
@Tag(name = "签名管理", description = "短信签名CRUD与审核")
@RequiredArgsConstructor
public class SignatureController {

    private final SignatureService signatureService;

    @Operation(summary = "获取签名列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> getList(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) @Parameter(description = "状态：0待审/1通过/2驳回") Integer status) {
        Map<String, Object> data = new HashMap<>();
        data.put("list", signatureService.getList(keyword, status, page, size));
        data.put("total", signatureService.getCount(keyword, status));
        data.put("page", page);
        data.put("size", size);
        return Result.success(data);
    }

    @Operation(summary = "获取签名详情")
    @GetMapping("/{id}")
    public Result<Signature> get(@PathVariable Long id) {
        return Result.success(signatureService.getById(id));
    }

    @Operation(summary = "创建签名")
    @PostMapping
    @Audit(module = "SIGNATURE", action = "CREATE")
    public Result<String> create(@Valid @RequestBody Signature signature) {
        signatureService.create(signature);
        return Result.success("签名创建成功，待审核");
    }

    @Operation(summary = "更新签名")
    @PutMapping("/{id}")
    @Audit(module = "SIGNATURE", action = "UPDATE")
    public Result<String> update(@PathVariable Long id, @Valid @RequestBody Signature signature) {
        signature.setId(id);
        signatureService.update(signature);
        return Result.success("签名更新成功");
    }

    @Operation(summary = "审核通过")
    @PutMapping("/{id}/approve")
    @Audit(module = "SIGNATURE", action = "APPROVE")
    public Result<String> approve(@PathVariable Long id, @RequestBody(required = false) RemarkRequest request) {
        signatureService.approve(id, request != null ? request.getRemark() : null);
        return Result.success("签名审核通过");
    }

    @Operation(summary = "审核驳回")
    @PutMapping("/{id}/reject")
    @Audit(module = "SIGNATURE", action = "REJECT")
    public Result<String> reject(@PathVariable Long id, @RequestBody(required = false) RemarkRequest request) {
        signatureService.reject(id, request != null ? request.getRemark() : null);
        return Result.success("签名已驳回");
    }

    @Operation(summary = "删除签名")
    @DeleteMapping("/{id}")
    @Audit(module = "SIGNATURE", action = "DELETE")
    public Result<String> delete(@PathVariable Long id) {
        signatureService.delete(id);
        return Result.success("签名删除成功");
    }

    @Data
    public static class RemarkRequest {
        /** 审核备注 */
        private String remark;
    }
}
