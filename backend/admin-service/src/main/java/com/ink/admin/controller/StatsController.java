package com.ink.admin.controller;

import com.ink.admin.client.ApiServiceClient;
import com.ink.admin.mapper.StatsMapper;
import com.ink.common.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据统计控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/stats")
@Tag(name = "数据统计", description = "看板概览、趋势与通道/客户维度统计")
@RequiredArgsConstructor
public class StatsController {

    private final StatsMapper statsMapper;
    private final ApiServiceClient apiServiceClient;

    @Operation(summary = "看板概览")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        Map<String, Object> data = new HashMap<>();
        data.put("spTotal", statsMapper.countSp());
        data.put("spEnabled", statsMapper.countEnabledSp());
        data.put("channelTotal", statsMapper.countChannel());
        data.put("channelEnabled", statsMapper.countEnabledChannel());
        data.put("channelOnline", countOnlineChannels());
        data.put("adminTotal", statsMapper.countAdmin());
        data.put("todayTotal", statsMapper.countTodayTotal());
        data.put("todaySuccess", statsMapper.countTodaySuccess());
        data.put("todayFail", statsMapper.countTodayFail());
        data.put("todayActiveSp", statsMapper.countTodayActiveSp());
        return Result.success(data);
    }

    @Operation(summary = "按天趋势")
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> trend(
            @RequestParam(value = "days", defaultValue = "7") @Parameter(description = "天数(7/30)") int days) {
        return Result.success(statsMapper.trendByDay(startDate(days)));
    }

    @Operation(summary = "按通道统计")
    @GetMapping("/channel")
    public Result<List<Map<String, Object>>> channelStats(
            @RequestParam(value = "days", defaultValue = "7") @Parameter(description = "天数") int days) {
        List<Map<String, Object>> list = statsMapper.statsByChannel(startDate(days));
        list.forEach(row -> row.put("successRate", successRate(row)));
        return Result.success(list);
    }

    @Operation(summary = "按客户统计")
    @GetMapping("/sp")
    public Result<List<Map<String, Object>>> spStats(
            @RequestParam(value = "days", defaultValue = "7") @Parameter(description = "天数") int days) {
        List<Map<String, Object>> list = statsMapper.statsBySp(startDate(days));
        list.forEach(row -> row.put("successRate", successRate(row)));
        return Result.success(list);
    }

    /**
     * 统计起始时间：近 days 天（含今天）的零点
     */
    private LocalDateTime startDate(int days) {
        int safeDays = days <= 0 ? 7 : Math.min(days, 365);
        return LocalDate.now().minusDays(safeDays - 1L).atStartOfDay();
    }

    /**
     * 计算成功率（保留两位小数）
     */
    private BigDecimal successRate(Map<String, Object> row) {
        long total = toLong(row.get("total"));
        if (total <= 0) {
            return BigDecimal.ZERO;
        }
        long success = toLong(row.get("success"));
        return BigDecimal.valueOf(success * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
    }

    private long toLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    /**
     * 在线通道数：connected（已建立连接数）> 0 的通道数
     */
    private long countOnlineChannels() {
        return apiServiceClient.getChannelStatus().values().stream()
                .filter(status -> status.get("connected") instanceof Number
                        && ((Number) status.get("connected")).intValue() > 0)
                .count();
    }
}
