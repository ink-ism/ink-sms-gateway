package com.ink.api.controller;

import com.ink.common.utils.Result;
import com.ink.core.connection.CmppConnectionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 通道刷新控制器
 * 提供通道热加载和状态查询接口
 */
@Slf4j
@RestController
@RequestMapping("/api/sms/channels")
@RequiredArgsConstructor
public class ChannelRefreshController {

    private final CmppConnectionManager connectionManager;

    /**
     * 刷新通道配置（从数据库重新加载）
     */
    @PostMapping("/refresh")
    public Result<String> refreshChannels() {
        connectionManager.refreshChannels();
        return Result.success("通道刷新完成");
    }

    /**
     * 查询各通道连接状态
     */
    @GetMapping("/status")
    public Result<Map<String, Map<String, Object>>> getChannelStatus() {
        return Result.success(connectionManager.getChannelStatus());
    }
}
