package com.ink.admin.mapper;

import com.ink.admin.dto.SmsDownDetail;
import com.ink.admin.entity.SmsDown;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 下行短信记录 Mapper
 */
public interface SmsDownMapper {

    List<SmsDown> findByPage(@Param("offset") int offset, @Param("size") int size);

    List<SmsDown> search(@Param("keyword") String keyword, @Param("offset") int offset, @Param("size") int size);

    int count();

    int countByKeyword(@Param("keyword") String keyword);

    int insert(SmsDown smsDown);

    int updateStatus(@Param("id") Long id, @Param("status") int status, @Param("statusReport") String statusReport, @Param("errorMsg") String errorMsg);

    SmsDownDetail findDetailById(@Param("id") Long id);
}
