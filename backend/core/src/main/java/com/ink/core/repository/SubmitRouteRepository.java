package com.ink.core.repository;

/**
 * 下行发送路由查询接口（数据库兜底）
 * 用于服务重启后内存路由缺失时，通过 server_msg_id 反查发送客户
 */
public interface SubmitRouteRepository {

    /**
     * 按服务端消息 ID（十六进制）查询发送客户标识
     * @return sp_id（REST 发送为 "REST"）；未找到返回 null
     */
    String findSpIdByServerMsgId(String serverMsgIdHex);
}
