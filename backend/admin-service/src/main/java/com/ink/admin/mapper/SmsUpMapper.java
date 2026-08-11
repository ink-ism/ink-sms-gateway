package com.ink.admin.mapper;

import com.ink.admin.dto.SmsUpDetail;
import com.ink.admin.entity.SmsUp;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 上行短信记录 Mapper
 */
public interface SmsUpMapper {

    List<SmsUp> findByPage(@Param("offset") int offset, @Param("size") int size);

    List<SmsUp> search(@Param("keyword") String keyword, @Param("offset") int offset, @Param("size") int size);

    int count();

    int countByKeyword(@Param("keyword") String keyword);

    int insert(SmsUp smsUp);

    /** 根据 ID 删除上行短信记录 */
    int deleteById(@Param("id") Long id);

    /** 查询上行短信详情（含关联下行短信与通道信息） */
    SmsUpDetail findDetailById(@Param("id") Long id);
}
