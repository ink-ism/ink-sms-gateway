package com.ink.admin.service;

import com.ink.admin.entity.Signature;
import com.ink.admin.mapper.SignatureMapper;
import com.ink.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 短信签名管理服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignatureService {

    private final SignatureMapper signatureMapper;

    public List<Signature> getList(String keyword, Integer status, int page, int size) {
        return signatureMapper.findByPage(keyword, status, (page - 1) * size, size);
    }

    public int getCount(String keyword, Integer status) {
        return signatureMapper.count(keyword, status);
    }

    public Signature getById(Long id) {
        Signature signature = signatureMapper.findById(id);
        if (signature == null) {
            throw new BusinessException("签名不存在");
        }
        return signature;
    }

    @Transactional
    public void create(Signature signature) {
        if (signature.getContent() == null || signature.getContent().isBlank()) {
            throw new BusinessException("签名内容不能为空");
        }
        if (signatureMapper.existsByContent(signature.getContent().trim(), null)) {
            throw new BusinessException("签名内容已存在");
        }
        signature.setContent(signature.getContent().trim());
        signature.setStatus(0);
        signature.setCreateTime(LocalDateTime.now());
        signature.setUpdateTime(LocalDateTime.now());
        if (signatureMapper.insert(signature) <= 0) {
            throw new BusinessException("创建签名失败");
        }
        log.info("签名创建成功: id={}, content={}", signature.getId(), signature.getContent());
    }

    @Transactional
    public void update(Signature signature) {
        Signature existing = getById(signature.getId());
        if (signature.getContent() == null || signature.getContent().isBlank()) {
            throw new BusinessException("签名内容不能为空");
        }
        if (signatureMapper.existsByContent(signature.getContent().trim(), signature.getId())) {
            throw new BusinessException("签名内容已存在");
        }
        signature.setContent(signature.getContent().trim());
        signature.setUpdateTime(LocalDateTime.now());
        // 内容变更后重置为待审核
        if (!existing.getContent().equals(signature.getContent())) {
            signature.setStatus(0);
        } else if (signature.getStatus() == null) {
            signature.setStatus(existing.getStatus());
        }
        if (signatureMapper.update(signature) <= 0) {
            throw new BusinessException("更新签名失败");
        }
        log.info("签名更新成功: id={}, content={}", signature.getId(), signature.getContent());
    }

    /**
     * 审核通过
     */
    @Transactional
    public void approve(Long id, String remark) {
        getById(id);
        signatureMapper.updateStatus(id, 1, remark);
        log.info("签名审核通过: id={}", id);
    }

    /**
     * 审核驳回
     */
    @Transactional
    public void reject(Long id, String remark) {
        getById(id);
        signatureMapper.updateStatus(id, 2, remark);
        log.info("签名审核驳回: id={}", id);
    }

    @Transactional
    public void delete(Long id) {
        getById(id);
        signatureMapper.deleteById(id);
        log.info("签名删除成功: id={}", id);
    }
}
