package com.ink.admin.service;

import com.ink.admin.entity.SmsUp;
import com.ink.admin.mapper.SmsUpMapper;
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
}
