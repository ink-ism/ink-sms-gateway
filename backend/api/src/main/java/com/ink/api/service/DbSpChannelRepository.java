package com.ink.api.service;

import com.ink.core.repository.SpChannelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 客户-通道绑定查询实现（ink_sp_channel 表）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DbSpChannelRepository implements SpChannelRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<String> findChannelCodes(String spId) {
        if (spId == null || spId.isBlank()) {
            return null;
        }
        try {
            return jdbcTemplate.queryForList(
                    "SELECT channel_code FROM ink_sp_channel WHERE sp_id = ?",
                    String.class, spId);
        } catch (Exception e) {
            log.error("查询客户绑定通道失败: spId={}, error={}", spId, e.getMessage());
            return null;
        }
    }
}
