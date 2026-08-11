package com.ink.admin.controller;

import com.ink.admin.audit.Audit;
import com.ink.admin.entity.Sp;
import com.ink.admin.service.SpService;
import com.ink.common.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 下游客户管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/sp")
@Tag(name = "客户管理", description = "下游SP客户配置相关接口")
@Validated
@RequiredArgsConstructor
public class SpController {

    private final SpService spService;

    @Operation(summary = "获取客户列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> getSpList(
            @RequestParam(value = "page", defaultValue = "1") @Parameter(description = "页码") int page,
            @RequestParam(value = "size", defaultValue = "10") @Parameter(description = "每页大小") int size,
            @RequestParam(value = "keyword", required = false) @Parameter(description = "搜索关键字") String keyword) {

        Map<String, Object> data = new HashMap<>();
        data.put("list", spService.getSpList(keyword, page, size));
        data.put("total", spService.getSpCount(keyword));
        data.put("page", page);
        data.put("size", size);
        return Result.success(data);
    }

    @Operation(summary = "获取客户详情")
    @GetMapping("/{id}")
    public Result<Sp> getSp(@PathVariable @Parameter(description = "客户ID") Long id) {
        return Result.success(spService.getSpById(id));
    }

    @Operation(summary = "创建客户")
    @PostMapping
    @Audit(module = "SP", action = "CREATE")
    public Result<String> createSp(@Valid @RequestBody Sp sp) {
        spService.createSp(sp);
        return Result.success("客户创建成功");
    }

    @Operation(summary = "更新客户")
    @PutMapping("/{id}")
    @Audit(module = "SP", action = "UPDATE")
    public Result<String> updateSp(@PathVariable @Parameter(description = "客户ID") Long id,
                                   @Valid @RequestBody Sp sp) {
        sp.setId(id);
        spService.updateSp(sp);
        return Result.success("客户更新成功");
    }

    @Operation(summary = "删除客户")
    @DeleteMapping("/{id}")
    @Audit(module = "SP", action = "DELETE")
    public Result<String> deleteSp(@PathVariable @Parameter(description = "客户ID") Long id) {
        spService.deleteSp(id);
        return Result.success("客户删除成功");
    }

    @Operation(summary = "启用客户")
    @PutMapping("/{id}/enable")
    @Audit(module = "SP", action = "ENABLE")
    public Result<String> enableSp(@PathVariable @Parameter(description = "客户ID") Long id) {
        spService.enableSp(id);
        return Result.success("客户已启用");
    }

    @Operation(summary = "禁用客户")
    @PutMapping("/{id}/disable")
    @Audit(module = "SP", action = "DISABLE")
    public Result<String> disableSp(@PathVariable @Parameter(description = "客户ID") Long id) {
        spService.disableSp(id);
        return Result.success("客户已禁用");
    }

    @Operation(summary = "获取客户绑定通道")
    @GetMapping("/{id}/channels")
    public Result<List<String>> getSpChannels(@PathVariable @Parameter(description = "客户ID") Long id) {
        return Result.success(spService.getChannelCodes(id));
    }

    @Operation(summary = "全量替换客户绑定通道")
    @PutMapping("/{id}/channels")
    @Audit(module = "SP", action = "UPDATE")
    public Result<String> bindChannels(@PathVariable @Parameter(description = "客户ID") Long id,
                                       @RequestBody ChannelBindRequest request) {
        spService.bindChannels(id, request.getChannelCodes());
        return Result.success("通道绑定更新成功");
    }

    @Operation(summary = "客户充值/调账")
    @PostMapping("/{id}/recharge")
    @Audit(module = "SP", action = "RECHARGE")
    public Result<Map<String, Object>> recharge(@PathVariable @Parameter(description = "客户ID") Long id,
                                                @Valid @RequestBody RechargeRequest request) {
        java.math.BigDecimal balanceAfter = spService.recharge(id, request.getAmount(), request.getRemark());
        Map<String, Object> data = new HashMap<>();
        data.put("balanceAfter", balanceAfter);
        return Result.success(data);
    }

    @Operation(summary = "获取客户余额流水")
    @GetMapping("/{id}/transactions")
    public Result<Map<String, Object>> getTransactions(
            @PathVariable @Parameter(description = "客户ID") Long id,
            @RequestParam(value = "page", defaultValue = "1") @Parameter(description = "页码") int page,
            @RequestParam(value = "size", defaultValue = "10") @Parameter(description = "每页大小") int size) {
        Map<String, Object> data = new HashMap<>();
        data.put("list", spService.getTransactions(id, page, size));
        data.put("total", spService.getTransactionCount(id));
        data.put("page", page);
        data.put("size", size);
        return Result.success(data);
    }

    @Data
    public static class ChannelBindRequest {
        /** 绑定的通道编码列表（全量替换） */
        private List<String> channelCodes;
    }

    @Data
    public static class RechargeRequest {
        /** 充值金额（正数充值，负数调账扣减） */
        @NotNull(message = "充值金额不能为空")
        private java.math.BigDecimal amount;
        /** 备注 */
        private String remark;
    }
}
