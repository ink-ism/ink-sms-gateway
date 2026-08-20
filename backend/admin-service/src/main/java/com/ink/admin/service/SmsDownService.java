package com.ink.admin.service;

import com.ink.admin.dto.SmsDownDetail;
import com.ink.admin.entity.SmsDown;
import com.ink.admin.mapper.SmsDownMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 下行短信记录服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsDownService {

    private final SmsDownMapper smsDownMapper;

    public List<SmsDown> getList(int page, int size) {
        return smsDownMapper.findByPage((page - 1) * size, size);
    }

    public List<SmsDown> search(String keyword, int page, int size) {
        return smsDownMapper.search(keyword, (page - 1) * size, size);
    }

    public int getCount() {
        return smsDownMapper.count();
    }

    public int getCountByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return smsDownMapper.count();
        }
        return smsDownMapper.countByKeyword(keyword);
    }

    public SmsDownDetail getDetail(Long id) {
        return smsDownMapper.findDetailById(id);
    }
}
