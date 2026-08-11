package com.ink.admin.mapper;

import com.ink.admin.entity.Blacklist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通道级退订黑名单Mapper接口
 */
@Mapper
public interface BlacklistMapper {

    /** 根据ID查询 */
    Blacklist findById(@Param("id") Long id);

    /** 分页查询（支持通道/手机号过滤） */
    List<Blacklist> findByPage(@Param("channelCode") String channelCode,
                               @Param("phone") String phone,
                               @Param("offset") int offset,
                               @Param("limit") int limit);

    /** 查询总数（支持通道/手机号过滤） */
    int count(@Param("channelCode") String channelCode, @Param("phone") String phone);

    /** 删除黑名单 */
    int deleteById(@Param("id") Long id);

    /** 新增或更新黑名单（按 channel_code + phone 去重） */
    int upsert(Blacklist blacklist);

    /** 查询指定通道+手机号是否已在黑名单中 */
    int existsByChannelAndPhone(@Param("channelCode") String channelCode, @Param("phone") String phone);
}
