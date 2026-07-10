package com.ink.admin.mapper;

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
}
