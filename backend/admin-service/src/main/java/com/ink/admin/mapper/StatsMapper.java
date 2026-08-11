package com.ink.admin.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 统计查询 Mapper
 * 数据来源 ink_sms_down（status：1-发送成功, 3-投递成功 视为成功）
 */
@Mapper
public interface StatsMapper {

    @Select("SELECT COUNT(*) FROM ink_sp")
    int countSp();

    @Select("SELECT COUNT(*) FROM ink_sp WHERE status = 1")
    int countEnabledSp();

    @Select("SELECT COUNT(*) FROM ink_channel")
    int countChannel();

    @Select("SELECT COUNT(*) FROM ink_channel WHERE status = 1")
    int countEnabledChannel();

    @Select("SELECT COUNT(*) FROM ink_admin_user")
    int countAdmin();

    @Select("SELECT COUNT(*) FROM ink_sms_down WHERE create_time >= CURDATE()")
    long countTodayTotal();

    @Select("SELECT COUNT(*) FROM ink_sms_down WHERE create_time >= CURDATE() AND status IN (1, 3)")
    long countTodaySuccess();

    @Select("SELECT COUNT(*) FROM ink_sms_down WHERE create_time >= CURDATE() AND status = 2")
    long countTodayFail();

    @Select("SELECT COUNT(DISTINCT sp_id) FROM ink_sms_down WHERE create_time >= CURDATE() AND sp_id IS NOT NULL")
    int countTodayActiveSp();

    /**
     * 按天聚合发送量/成功量（startDate 含当日零点）
     */
    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS date, " +
            "COUNT(*) AS total, " +
            "SUM(CASE WHEN status IN (1, 3) THEN 1 ELSE 0 END) AS success " +
            "FROM ink_sms_down " +
            "WHERE create_time >= #{startDate} " +
            "GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d') ORDER BY date")
    List<Map<String, Object>> trendByDay(@Param("startDate") LocalDateTime startDate);

    /**
     * 按通道聚合发送量/成功量/失败量
     */
    @Select("SELECT IFNULL(channel_code, 'UNKNOWN') AS channelCode, " +
            "COUNT(*) AS total, " +
            "SUM(CASE WHEN status IN (1, 3) THEN 1 ELSE 0 END) AS success, " +
            "SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) AS fail " +
            "FROM ink_sms_down " +
            "WHERE create_time >= #{startDate} " +
            "GROUP BY IFNULL(channel_code, 'UNKNOWN') ORDER BY total DESC")
    List<Map<String, Object>> statsByChannel(@Param("startDate") LocalDateTime startDate);

    /**
     * 按客户聚合发送量/成功量/消费金额
     */
    @Select("SELECT IFNULL(sp_id, 'UNKNOWN') AS spId, " +
            "COUNT(*) AS total, " +
            "SUM(CASE WHEN status IN (1, 3) THEN 1 ELSE 0 END) AS success, " +
            "COALESCE(SUM(fee), 0) AS fee " +
            "FROM ink_sms_down " +
            "WHERE create_time >= #{startDate} " +
            "GROUP BY IFNULL(sp_id, 'UNKNOWN') ORDER BY total DESC")
    List<Map<String, Object>> statsBySp(@Param("startDate") LocalDateTime startDate);
}
