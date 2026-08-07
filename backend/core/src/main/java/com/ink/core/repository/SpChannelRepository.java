package com.ink.core.repository;

import java.util.List;

/**
 * 客户-通道绑定查询接口
 * core 定义、api 侧实现（数据库驱动）
 */
public interface SpChannelRepository {

    /**
     * 查询客户绑定的通道编码列表
     * @param spId 客户标识
     * @return 绑定的通道编码；返回 null 或空列表表示不限制（允许全部启用通道）
     */
    List<String> findChannelCodes(String spId);
}
