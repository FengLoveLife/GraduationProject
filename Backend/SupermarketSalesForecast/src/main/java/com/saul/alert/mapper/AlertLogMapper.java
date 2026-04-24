package com.saul.alert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.saul.alert.entity.AlertLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface AlertLogMapper extends BaseMapper<AlertLog> {

    /**
     * INSERT IGNORE：依赖唯一索引 uk_alert_dedup(alert_type, product_id, biz_date) 去重。
     * 重复行静默跳过，返回 0；新增成功返回 1。不抛异常，无需事务回滚保护。
     */
    @Insert("INSERT IGNORE INTO alert_log " +
            "(alert_type, product_id, product_name, alert_content, is_read, biz_date, create_time) " +
            "VALUES (#{alertType}, #{productId}, #{productName}, #{alertContent}, #{isRead}, #{bizDate}, #{createTime})")
    int insertIgnore(AlertLog log);

    /** 全部标为已读 */
    @Update("UPDATE alert_log SET is_read = 1 WHERE is_read = 0")
    void markAllRead();

    /**
     * 查询指定节假日日期列表中每个商品的预测总需求量。
     * 用于节假日预警：将多天预测量合并后与当前库存比较。
     */
    @Select({
        "<script>",
        "SELECT product_id AS productId, product_name AS productName,",
        "SUM(predicted_quantity) AS totalDemand",
        "FROM forecast_result",
        "WHERE forecast_date IN",
        "<foreach item='d' collection='dates' open='(' separator=',' close=')'>",
        "#{d}",
        "</foreach>",
        "GROUP BY product_id, product_name",
        "</script>"
    })
    List<Map<String, Object>> getHolidayDemandByDates(@Param("dates") List<LocalDate> dates);

    /** 查询今日各类型预警汇总数（用于角标显示） */
    @Select("SELECT COUNT(*) FROM alert_log WHERE is_read = 0")
    int countUnread();
}
