package com.ink.admin.mapper;

import com.ink.admin.entity.SensitiveWord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 敏感词 Mapper
 */
@Mapper
public interface SensitiveWordMapper {

    SensitiveWord findById(@Param("id") Long id);

    List<SensitiveWord> findByPage(@Param("keyword") String keyword,
                                   @Param("status") Integer status,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit);

    int count(@Param("keyword") String keyword, @Param("status") Integer status);

    boolean existsByWord(@Param("word") String word, @Param("excludeId") Long excludeId);

    int insert(SensitiveWord sensitiveWord);

    int update(SensitiveWord sensitiveWord);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int deleteById(@Param("id") Long id);
}
