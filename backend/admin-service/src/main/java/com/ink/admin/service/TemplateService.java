package com.ink.admin.service;

import com.ink.admin.entity.Template;
import com.ink.admin.mapper.TemplateMapper;
import com.ink.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 短信模板管理服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateMapper templateMapper;

    public List<Template> getList(String keyword, Integer status, int page, int size) {
        return templateMapper.findByPage(keyword, status, (page - 1) * size, size);
    }

    public int getCount(String keyword, Integer status) {
        return templateMapper.count(keyword, status);
    }

    public Template getById(Long id) {
        Template template = templateMapper.findById(id);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        return template;
    }

    @Transactional
    public void create(Template template) {
        validate(template);
        template.setStatus(0);
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        if (templateMapper.insert(template) <= 0) {
            throw new BusinessException("创建模板失败");
        }
        log.info("模板创建成功: id={}, name={}", template.getId(), template.getName());
    }

    @Transactional
    public void update(Template template) {
        Template existing = getById(template.getId());
        validate(template);
        template.setUpdateTime(LocalDateTime.now());
        // 内容变更后重置为待审核
        if (!existing.getContent().equals(template.getContent())) {
            template.setStatus(0);
        } else if (template.getStatus() == null) {
            template.setStatus(existing.getStatus());
        }
        if (templateMapper.update(template) <= 0) {
            throw new BusinessException("更新模板失败");
        }
        log.info("模板更新成功: id={}, name={}", template.getId(), template.getName());
    }

    /**
     * 审核通过
     */
    @Transactional
    public void approve(Long id, String remark) {
        getById(id);
        templateMapper.updateStatus(id, 1, remark);
        log.info("模板审核通过: id={}", id);
    }

    /**
     * 审核驳回
     */
    @Transactional
    public void reject(Long id, String remark) {
        getById(id);
        templateMapper.updateStatus(id, 2, remark);
        log.info("模板审核驳回: id={}", id);
    }

    @Transactional
    public void delete(Long id) {
        getById(id);
        templateMapper.deleteById(id);
        log.info("模板删除成功: id={}", id);
    }

    private void validate(Template template) {
        if (template.getName() == null || template.getName().isBlank()) {
            throw new BusinessException("模板名称不能为空");
        }
        if (template.getContent() == null || template.getContent().isBlank()) {
            throw new BusinessException("模板内容不能为空");
        }
    }
}
