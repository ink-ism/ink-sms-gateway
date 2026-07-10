package com.ink.gateway.controller;

import com.ink.common.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 网关控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/gateway")
public class GatewayController {

    @Value("${server.port}")
    private String port;

    @GetMapping("/status")
    public Result<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("gateway", "running");
        status.put("port", port);
        status.put("timestamp", System.currentTimeMillis());
        
        log.info("Gateway status request received");
        return Result.success(status);
    }

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("gateway", "gateway-service");
        health.put("timestamp", System.currentTimeMillis());
        
        log.info("Gateway health check");
        return Result.success(health);
    }
}