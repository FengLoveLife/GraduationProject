package com.saul.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日历影响因子实体
 */
@Data
@TableName("calendar_factor")
public class CalendarFactor implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 日期(唯一) */
    private LocalDate date;

    /** 星期几 1-7(1=周一) */
    private Integer dayOfWeek;

    /** 是否周末: 0否 1是 */
    private Integer isWeekend;

    /** 是否节假日: 0否 1是 */
    private Integer isHoliday;

    /** 节假日名称: 春节等 */
    private String holidayName;

    /** 天气: 晴/多云/雨/雪 */
    private String weather;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
