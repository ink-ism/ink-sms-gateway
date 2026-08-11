package com.ink.admin.mapper;

import com.ink.admin.entity.Signature;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 短信签名 Mapper
 */
@Mapper
public interface SignatureMapper {

    Signature findById(@Param("id") Long id);

    List<Signature> findByPage(@Param("keyword") String keyword,
                               @Param("status") Integer status,
                               @Param("offset") int offset,
                               @Param("limit") int limit);

    int count(@Param("keyword") String keyword, @Param("status") Integer status);

    boolean existsByContent(@Param("content") String content, @Param("excludeId") Long excludeId);

    int insert(Signature signature);

    int update(Signature signature);

    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("remark") String remark);

    int deleteById(@Param("id") Long id);
}
