package com.ink.admin.service;

import com.ink.admin.client.ApiServiceClient;
import com.ink.admin.entity.SensitiveWord;
import com.ink.admin.mapper.SensitiveWordMapper;
import com.ink.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 敏感词管理服务类
 * 增删改后通知 api 模块刷新内存词表
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensitiveWordService {

    private final SensitiveWordMapper sensitiveWordMapper;
    private final ApiServiceClient apiServiceClient;

    public List<SensitiveWord> getList(String keyword, Integer status, int page, int size) {
        return sensitiveWordMapper.findByPage(keyword, status, (page - 1) * size, size);
    }

    public int getCount(String keyword, Integer status) {
        return sensitiveWordMapper.count(keyword, status);
    }

    @Transactional
    public void create(SensitiveWord word) {
        if (word.getWord() == null || word.getWord().isBlank()) {
            throw new BusinessException("敏感词不能为空");
        }
        String trimmed = word.getWord().trim();
        if (sensitiveWordMapper.existsByWord(trimmed, null)) {
            throw new BusinessException("敏感词已存在");
        }
        word.setWord(trimmed);
        if (word.getStatus() == null) {
            word.setStatus(1);
        }
        word.setCreateTime(LocalDateTime.now());
        if (sensitiveWordMapper.insert(word) <= 0) {
            throw new BusinessException("创建敏感词失败");
        }
        log.info("敏感词创建成功: id={}, word={}", word.getId(), word.getWord());
        notifyRefresh();
    }

    @Transactional
    public void update(SensitiveWord word) {
        SensitiveWord existing = getById(word.getId());
        if (word.getWord() == null || word.getWord().isBlank()) {
            throw new BusinessException("敏感词不能为空");
        }
        String trimmed = word.getWord().trim();
        if (sensitiveWordMapper.existsByWord(trimmed, word.getId())) {
            throw new BusinessException("敏感词已存在");
        }
        word.setWord(trimmed);
        if (word.getStatus() == null) {
            word.setStatus(existing.getStatus());
        }
        if (sensitiveWordMapper.update(word) <= 0) {
            throw new BusinessException("更新敏感词失败");
        }
        log.info("敏感词更新成功: id={}, word={}", word.getId(), word.getWord());
        notifyRefresh();
    }

    @Transactional
    public void enable(Long id) {
        getById(id);
        sensitiveWordMapper.updateStatus(id, 1);
        log.info("敏感词启用: id={}", id);
        notifyRefresh();
    }

    @Transactional
    public void disable(Long id) {
        getById(id);
        sensitiveWordMapper.updateStatus(id, 0);
        log.info("敏感词禁用: id={}", id);
        notifyRefresh();
    }

    @Transactional
    public void delete(Long id) {
        getById(id);
        sensitiveWordMapper.deleteById(id);
        log.info("敏感词删除成功: id={}", id);
        notifyRefresh();
    }

    private SensitiveWord getById(Long id) {
        SensitiveWord word = sensitiveWordMapper.findById(id);
        if (word == null) {
            throw new BusinessException("敏感词不存在");
        }
        return word;
    }

    /** 事务提交后再通知 api 刷新，避免 api 读到未提交的数据 */
    private void notifyRefresh() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    apiServiceClient.refreshSensitiveWords();
                }
            });
        } else {
            apiServiceClient.refreshSensitiveWords();
        }
    }
}
