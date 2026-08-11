package com.ink.api.controller;

import com.ink.api.service.SensitiveWordService;
import com.ink.common.utils.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 敏感词刷新控制器
 * 供 admin-service 在敏感词增删改后调用，重载内存词表
 */
@Slf4j
@RestController
@RequestMapping("/api/sms/sensitives")
@RequiredArgsConstructor
public class SensitiveWordRefreshController {

    private final SensitiveWordService sensitiveWordService;

    /**
     * 重新加载敏感词表
     */
    @PostMapping("/refresh")
    public Result<String> refresh() {
        sensitiveWordService.reload();
        return Result.success("敏感词刷新完成");
    }
}
