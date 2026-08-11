package com.ink.admin.service;

import com.ink.admin.dto.SmsUpDetail;
import com.ink.admin.entity.SmsUp;
import com.ink.admin.mapper.SmsUpMapper;
import com.ink.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 上行短信记录服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsUpService {

    private final SmsUpMapper smsUpMapper;

    public List<SmsUp> getList(int page, int size) {
        return smsUpMapper.findByPage((page - 1) * size, size);
    }

    public List<SmsUp> search(String keyword, int page, int size) {
        return smsUpMapper.search(keyword, (page - 1) * size, size);
    }

    public int getCount() {
        return smsUpMapper.count();
    }

    public int getCountByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return smsUpMapper.count();
        }
        return smsUpMapper.countByKeyword(keyword);
    }

    /**
     * 删除上行短信记录
     */
    public void deleteById(Long id) {
        int rows = smsUpMapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException("上行短信记录不存在");
        }
        log.info("上行短信记录已删除: id={}", id);
    }

    /**
     * 查询上行短信详情（含关联下行短信与通道信息）
     */
    public SmsUpDetail getDetail(Long id) {
        SmsUpDetail detail = smsUpMapper.findDetailById(id);
        if (detail == null) {
            throw new BusinessException("上行短信记录不存在");
        }
        return detail;
    }
}

