package com.saul.sales.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.saul.sales.entity.CalendarFactor;
import com.saul.sales.mapper.CalendarFactorMapper;
import com.saul.sales.service.ICalendarFactorService;
import org.springframework.stereotype.Service;

/**
 * 日历影响因子表 Service 实现类
 */
@Service
public class CalendarFactorServiceImpl extends ServiceImpl<CalendarFactorMapper, CalendarFactor>
        implements ICalendarFactorService {
}