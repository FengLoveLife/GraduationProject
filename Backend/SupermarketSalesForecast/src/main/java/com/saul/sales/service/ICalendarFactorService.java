package com.saul.sales.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.saul.sales.dto.CalendarInitDTO;
import com.saul.sales.dto.CalendarUpdateDTO;
import com.saul.sales.entity.CalendarFactor;
import com.saul.sales.vo.CalendarFactorVO;

import java.util.List;

/**
 * 日历影响因子表 Service 接口
 */
public interface ICalendarFactorService extends IService<CalendarFactor> {

    /**
     * 按月份查询日历数据
     */
    List<CalendarFactorVO> getMonthData(Integer year, Integer month);

    /**
     * 初始化月份日历数据
     */
    String initMonthCalendar(CalendarInitDTO dto);

    /**
     * 更新日历因子
     */
    void updateFactor(CalendarUpdateDTO dto);

    /**
     * 按 ID 查询单条日历因子（返回 VO）
     */
    CalendarFactorVO getVOById(Long id);
}