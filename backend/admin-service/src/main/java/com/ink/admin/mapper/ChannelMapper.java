package com.ink.admin.mapper;

import com.ink.admin.entity.Channel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通道配置Mapper接口
 */
@Mapper
public interface ChannelMapper {

    /** 根据ID查询通道 */
    Channel findById(@Param("id") Long id);

    /** 根据编码查询通道 */
    Channel findByCode(@Param("code") String code);

    /** 查询所有通道 */
    List<Channel> findAll();

    /** 分页查询通道 */
    List<Channel> findByPage(@Param("offset") int offset, @Param("limit") int limit);

    /** 按关键字搜索通道 */
    List<Channel> search(@Param("keyword") String keyword, @Param("offset") int offset, @Param("limit") int limit);

    /** 查询通道总数 */
    int count();

    /** 按关键字搜索通道总数 */
    int countByKeyword(@Param("keyword") String keyword);

    /** 插入通道 */
    int insert(Channel channel);

    /** 更新通道 */
    int update(Channel channel);

    /** 删除通道 */
    int deleteById(@Param("id") Long id);

    /** 更新通道状态 */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 检查编码是否存在 */
    boolean existsByCode(@Param("code") String code);
}
