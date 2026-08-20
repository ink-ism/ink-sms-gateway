package com.ink.api.controller;

import com.ink.api.task.DailySmsTask;
import com.ink.common.utils.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 每日短信任务手动触发接口
 */
@Slf4j
@RestController
@RequestMapping("/api/sms/daily")
@RequiredArgsConstructor
public class DailySmsController {

    private final DailySmsTask dailySmsTask;

    /**
     * 手动触发每日短信发送任务
     */
    @PostMapping("/trigger")
    public Result<DailySmsTask.SendResult> trigger() {
        log.info("手动触发每日短信任务");
        DailySmsTask.SendResult result = dailySmsTask.doSend();
        return Result.success(result);
    }
}
