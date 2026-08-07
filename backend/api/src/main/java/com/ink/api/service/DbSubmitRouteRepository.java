package com.ink.api.service;

import com.ink.core.repository.SubmitRouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 下行发送路由数据库兜底查询实现
 * 服务重启后内存路由缺失时，通过 server_msg_id 反查发送客户
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DbSubmitRouteRepository implements SubmitRouteRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String findSpIdByServerMsgId(String serverMsgIdHex) {
        if (serverMsgIdHex == null || serverMsgIdHex.isBlank()) {
            return null;
        }
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT sp_id FROM ink_sms_down WHERE server_msg_id = ? ORDER BY create_time DESC LIMIT 1",
                    String.class, serverMsgIdHex);
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (Exception e) {
            log.error("路由兜底查询失败: serverMsgId={}, error={}", serverMsgIdHex, e.getMessage());
            return null;
        }
    }
}
