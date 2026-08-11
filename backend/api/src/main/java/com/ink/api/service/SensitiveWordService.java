package com.ink.api.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 敏感词服务
 * 启动时全量加载启用词表到内存，发送前做忽略大小写的包含匹配
 */
@Slf4j
@Service
@RequiredArgsConstructor
@DependsOn("smsRecordService")
public class SensitiveWordService {

    private final JdbcTemplate jdbcTemplate;

    /** 启用中的敏感词（统一小写存储） */
    private volatile Set<String> words = new CopyOnWriteArraySet<>();

    @PostConstruct
    public void init() {
        reload();
    }

    /**
     * 判断内容是否命中敏感词
     * @return 命中的敏感词原文；未命中返回 null
     */
    public String match(String content) {
        if (content == null || words.isEmpty()) {
            return null;
        }
        String lower = content.toLowerCase();
        for (String word : words) {
            if (lower.contains(word)) {
                return word;
            }
        }
        return null;
    }

    /**
     * 从数据库重新加载启用敏感词
     */
    public synchronized void reload() {
        try {
            List<String> list = jdbcTemplate.queryForList(
                    "SELECT word FROM ink_sensitive_word WHERE status = 1", String.class);
            Set<String> newSet = new CopyOnWriteArraySet<>();
            for (String word : list) {
                if (word != null && !word.isBlank()) {
                    newSet.add(word.trim().toLowerCase());
                }
            }
            words = newSet;
            log.info("已加载 {} 个启用敏感词", newSet.size());
        } catch (Exception e) {
            log.error("加载敏感词失败: {}", e.getMessage(), e);
        }
    }
}
