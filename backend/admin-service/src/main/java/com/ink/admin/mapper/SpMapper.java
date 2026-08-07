package com.ink.admin.mapper;

import com.ink.admin.entity.Sp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 下游客户Mapper接口
 */
@Mapper
public interface SpMapper {

    /** 根据ID查询客户 */
    Sp findById(@Param("id") Long id);

    /** 根据客户标识查询 */
    Sp findBySpId(@Param("spId") String spId);

    /** 分页查询客户 */
    List<Sp> findByPage(@Param("offset") int offset, @Param("limit") int limit);

    /** 按关键字搜索客户 */
    List<Sp> search(@Param("keyword") String keyword, @Param("offset") int offset, @Param("limit") int limit);

    /** 查询客户总数 */
    int count();

    /** 按关键字搜索客户总数 */
    int countByKeyword(@Param("keyword") String keyword);

    /** 插入客户 */
    int insert(Sp sp);

    /** 更新客户 */
    int update(Sp sp);

    /** 删除客户 */
    int deleteById(@Param("id") Long id);

    /** 更新客户状态 */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 检查客户标识是否存在 */
    boolean existsBySpId(@Param("spId") String spId);

    // ==================== 通道绑定 ====================

    /** 查询客户绑定的通道编码 */
    List<String> findChannelCodes(@Param("spId") String spId);

    /** 删除客户的全部通道绑定 */
    int deleteChannelsBySpId(@Param("spId") String spId);

    /** 新增客户通道绑定 */
    int insertChannel(@Param("spId") String spId, @Param("channelCode") String channelCode);
}
