package com.saul.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.saul.entity.CalendarFactor;
import com.saul.mapper.CalendarFactorMapper;
import com.saul.service.ICalendarFactorService;
import org.springframework.stereotype.Service;

/**
 * 日历影响因子表 Service 实现类
 */
@Service
public class CalendarFactorServiceImpl extends ServiceImpl<CalendarFactorMapper, CalendarFactor>
        implements ICalendarFactorService {
}
