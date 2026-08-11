package com.ink.admin.controller;

import com.ink.admin.audit.Audit;
import com.ink.admin.entity.Channel;
import com.ink.admin.service.ChannelService;
import com.ink.common.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通道管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/channel")
@Tag(name = "通道管理", description = "通道配置相关接口")
@Validated
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    @Operation(summary = "获取通道列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> getChannelList(
            @RequestParam(value = "page", defaultValue = "1") @Parameter(description = "页码") int page,
            @RequestParam(value = "size", defaultValue = "10") @Parameter(description = "每页大小") int size,
            @RequestParam(value = "keyword", required = false) @Parameter(description = "搜索关键字") String keyword) {

        List<Channel> channels;
        int total;
        if (keyword != null && !keyword.isBlank()) {
            channels = channelService.searchChannels(keyword, page, size);
            total = channelService.getChannelCountByKeyword(keyword);
        } else {
            channels = channelService.getChannelList(page, size);
            total = channelService.getChannelCount();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("list", channels);
        data.put("total", total);
        data.put("page", page);
        data.put("size", size);
        return Result.success(data);
    }

    @Operation(summary = "获取通道详情")
    @GetMapping("/{id}")
    public Result<Channel> getChannel(@PathVariable @Parameter(description = "通道ID") Long id) {
        Channel channel = channelService.getChannelById(id);
        return Result.success(channel);
    }

    @Operation(summary = "创建通道")
    @PostMapping
    @Audit(module = "CHANNEL", action = "CREATE")
    public Result<String> createChannel(@Valid @RequestBody Channel channel) {
        channelService.createChannel(channel);
        return Result.success("通道创建成功");
    }

    @Operation(summary = "更新通道")
    @PutMapping("/{id}")
    @Audit(module = "CHANNEL", action = "UPDATE")
    public Result<String> updateChannel(@PathVariable @Parameter(description = "通道ID") Long id,
                                        @Valid @RequestBody Channel channel) {
        channel.setId(id);
        channelService.updateChannel(channel);
        return Result.success("通道更新成功");
    }

    @Operation(summary = "删除通道")
    @DeleteMapping("/{id}")
    @Audit(module = "CHANNEL", action = "DELETE")
    public Result<String> deleteChannel(@PathVariable @Parameter(description = "通道ID") Long id) {
        channelService.deleteChannel(id);
        return Result.success("通道删除成功");
    }

    @Operation(summary = "启用通道")
    @PutMapping("/{id}/enable")
    @Audit(module = "CHANNEL", action = "ENABLE")
    public Result<String> enableChannel(@PathVariable @Parameter(description = "通道ID") Long id) {
        channelService.enableChannel(id);
        return Result.success("通道已启用");
    }

    @Operation(summary = "禁用通道")
    @PutMapping("/{id}/disable")
    @Audit(module = "CHANNEL", action = "DISABLE")
    public Result<String> disableChannel(@PathVariable @Parameter(description = "通道ID") Long id) {
        channelService.disableChannel(id);
        return Result.success("通道已禁用");
    }
}
