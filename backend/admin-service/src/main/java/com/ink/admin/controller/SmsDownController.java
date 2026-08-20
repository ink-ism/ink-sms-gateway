package com.ink.admin.controller;

import com.ink.admin.dto.SmsDownDetail;
import com.ink.admin.entity.SmsDown;
import com.ink.admin.service.SmsDownService;
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
 * 下行短信记录控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/sms/down")
@Tag(name = "下行短信", description = "下行短信记录查询接口")
@RequiredArgsConstructor
public class SmsDownController {

    private final SmsDownService smsDownService;

    @Operation(summary = "获取下行短信列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> getList(
            @RequestParam(value = "page", defaultValue = "1") @Parameter(description = "页码") int page,
            @RequestParam(value = "size", defaultValue = "10") @Parameter(description = "每页大小") int size,
            @RequestParam(value = "keyword", required = false) @Parameter(description = "搜索关键字") String keyword) {

        List<SmsDown> list;
        int total;
        if (keyword != null && !keyword.isBlank()) {
            list = smsDownService.search(keyword, page, size);
            total = smsDownService.getCountByKeyword(keyword);
        } else {
            list = smsDownService.getList(page, size);
            total = smsDownService.getCount();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        data.put("page", page);
        data.put("size", size);
        return Result.success(data);
    }

    @Operation(summary = "获取下行短信详情")
    @GetMapping("/{id}/detail")
    public Result<SmsDownDetail> getDetail(
            @PathVariable @Parameter(description = "下行短信ID") Long id) {
        return Result.success(smsDownService.getDetail(id));
    }
}
