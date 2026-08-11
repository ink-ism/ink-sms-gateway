package com.ink.admin.mapper;

import com.ink.admin.entity.Template;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 短信模板 Mapper
 */
@Mapper
public interface TemplateMapper {

    Template findById(@Param("id") Long id);

    List<Template> findByPage(@Param("keyword") String keyword,
                              @Param("status") Integer status,
                              @Param("offset") int offset,
                              @Param("limit") int limit);

    int count(@Param("keyword") String keyword, @Param("status") Integer status);

    int insert(Template template);

    int update(Template template);

    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("remark") String remark);

    int deleteById(@Param("id") Long id);
}
