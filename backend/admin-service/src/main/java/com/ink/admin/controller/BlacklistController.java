package com.ink.admin.controller;

import com.ink.admin.service.BlacklistManageService;
import com.ink.common.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 退订黑名单管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/blacklist")
@Tag(name = "黑名单管理", description = "通道级退订黑名单查询与移除接口")
@Validated
@RequiredArgsConstructor
public class BlacklistController {

    private final BlacklistManageService blacklistManageService;

    @Operation(summary = "分页查询黑名单")
    @GetMapping("/list")
    public Result<Map<String, Object>> getBlacklist(
            @RequestParam(value = "page", defaultValue = "1") @Parameter(description = "页码") int page,
            @RequestParam(value = "size", defaultValue = "10") @Parameter(description = "每页大小") int size,
            @RequestParam(value = "channelCode", required = false) @Parameter(description = "通道编码") String channelCode,
            @RequestParam(value = "phone", required = false) @Parameter(description = "手机号") String phone) {

        Map<String, Object> data = new HashMap<>();
        data.put("list", blacklistManageService.getBlacklist(channelCode, phone, page, size));
        data.put("total", blacklistManageService.getBlacklistCount(channelCode, phone));
        data.put("page", page);
        data.put("size", size);
        return Result.success(data);
    }

    @Operation(summary = "移除黑名单")
    @DeleteMapping("/{id}")
    public Result<String> removeBlacklist(@PathVariable @Parameter(description = "黑名单ID") Long id) {
        blacklistManageService.removeBlacklist(id);
        return Result.success("黑名单移除成功");
    }
}
