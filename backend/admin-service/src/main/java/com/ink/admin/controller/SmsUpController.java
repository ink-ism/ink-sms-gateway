package com.ink.admin.controller;

import com.ink.admin.dto.SmsUpDetail;
import com.ink.admin.entity.SmsUp;
import com.ink.admin.service.BlacklistManageService;
import com.ink.admin.service.SmsUpService;
import com.ink.common.exception.BusinessException;
import com.ink.common.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.ink.admin.audit.Audit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 上行短信记录控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/sms/up")
@Tag(name = "上行短信", description = "上行短信记录查询、详情、删除、加黑接口")
@RequiredArgsConstructor
public class SmsUpController {

    private final SmsUpService smsUpService;
    private final BlacklistManageService blacklistManageService;

    @Operation(summary = "获取上行短信列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> getList(
            @RequestParam(value = "page", defaultValue = "1") @Parameter(description = "页码") int page,
            @RequestParam(value = "size", defaultValue = "10") @Parameter(description = "每页大小") int size,
            @RequestParam(value = "keyword", required = false) @Parameter(description = "搜索关键字") String keyword) {

        List<SmsUp> list;
        int total;
        if (keyword != null && !keyword.isBlank()) {
            list = smsUpService.search(keyword, page, size);
            total = smsUpService.getCountByKeyword(keyword);
        } else {
            list = smsUpService.getList(page, size);
            total = smsUpService.getCount();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        data.put("page", page);
        data.put("size", size);
        return Result.success(data);
    }

    @Operation(summary = "获取上行短信详情")
    @GetMapping("/{id}/detail")
    public Result<SmsUpDetail> getDetail(
            @PathVariable @Parameter(description = "上行短信ID") Long id) {
        return Result.success(smsUpService.getDetail(id));
    }

    @Operation(summary = "删除上行短信记录")
    @DeleteMapping("/{id}")
    @Audit(module = "SMS_UP", action = "DELETE")
    public Result<String> delete(
            @PathVariable @Parameter(description = "上行短信ID") Long id) {
        smsUpService.deleteById(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "将上行手机号加入黑名单")
    @PostMapping("/{id}/blacklist")
    @Audit(module = "BLACKLIST", action = "CREATE")
    public Result<String> addToBlacklist(
            @PathVariable @Parameter(description = "上行短信ID") Long id) {
        // 先查出上行记录以获取 channelCode、srcTerminalId、msgId
        SmsUpDetail detail = smsUpService.getDetail(id);
        if (detail.getChannelCode() == null || detail.getChannelCode().isBlank()) {
            throw new BusinessException("该上行记录缺少通道信息，无法加黑");
        }
        blacklistManageService.addBlacklist(detail.getChannelCode(), detail.getSrcTerminalId(), detail.getMsgId());
        return Result.success("已加入黑名单");
    }
}
