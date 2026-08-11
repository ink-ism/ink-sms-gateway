package com.ink.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 签名模板校验服务（REST 发送入口用）
 * 校验签名/模板已通过审核，并组装最终发送内容
 */
@Slf4j
@Service
@RequiredArgsConstructor
@DependsOn("smsRecordService")
public class SignatureTemplateService {

    private final JdbcTemplate jdbcTemplate;

    /** 签名状态：已通过 */
    private static final int STATUS_APPROVED = 1;

    /**
     * 校验签名已通过审核
     * @return 签名内容；不存在或未通过返回 null
     */
    public String findApprovedSignature(Long signatureId) {
        if (signatureId == null) {
            return null;
        }
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT content FROM ink_signature WHERE id = ? AND status = ?",
                    String.class, signatureId, STATUS_APPROVED);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * 校验模板已通过审核
     * @return 模板内容；不存在或未通过返回 null
     */
    public String findApprovedTemplate(Long templateId) {
        if (templateId == null) {
            return null;
        }
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT content FROM ink_template WHERE id = ? AND status = ?",
                    String.class, templateId, STATUS_APPROVED);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * 组装发送内容：模板内容 + 签名
     * @param templateContent 模板内容（已通过审核）
     * @param signature       签名内容（已通过审核，可为 null 表示不拼接）
     */
    public String assembleContent(String templateContent, String signature) {
        if (signature == null || signature.isBlank()) {
            return templateContent;
        }
        // 签名已包含【】则直接拼接，否则包裹
        String sig = signature.startsWith("【") ? signature : "【" + signature + "】";
        return sig + templateContent;
    }
}
