package com.saul.sales.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.saul.sales.dto.CalendarInitDTO;
import com.saul.sales.dto.CalendarUpdateDTO;
import com.saul.sales.entity.CalendarFactor;
import com.saul.sales.mapper.CalendarFactorMapper;
import com.saul.sales.service.ICalendarFactorService;
import com.saul.sales.vo.CalendarFactorVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 日历影响因子表 Service 实现类
 */
@Service
@RequiredArgsConstructor
public class CalendarFactorServiceImpl extends ServiceImpl<CalendarFactorMapper, CalendarFactor>
        implements ICalendarFactorService {

    @Override
    public List<CalendarFactorVO> getMonthData(Integer year, Integer month) {
        // 构建月份的起止日期
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // 查询该月的数据
        List<CalendarFactor> list = this.list(new LambdaQueryWrapper<CalendarFactor>()
                .between(CalendarFactor::getDate, startDate, endDate)
                .orderByAsc(CalendarFactor::getDate));

        // 转换为VO
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public String initMonthCalendar(CalendarInitDTO dto) {
        YearMonth yearMonth = YearMonth.of(dto.getYear(), dto.getMonth());
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // 查出该月已存在的日期
        Set<LocalDate> existingDates = this.list(new LambdaQueryWrapper<CalendarFactor>()
                .between(CalendarFactor::getDate, startDate, endDate))
                .stream()
                .map(CalendarFactor::getDate)
                .collect(Collectors.toSet());

        // 生成新记录
        List<CalendarFactor> newFactors = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            if (!existingDates.contains(current)) {
                CalendarFactor factor = new CalendarFactor();
                factor.setDate(current);

                // 计算星期几 (1=周一, 7=周日)
                DayOfWeek dayOfWeek = current.getDayOfWeek();
                factor.setDayOfWeek(dayOfWeek.getValue());

                // 判断是否周末 (周六=6, 周日=7)
                factor.setIsWeekend(dayOfWeek.getValue() >= 6 ? 1 : 0);

                // 默认值
                factor.setIsHoliday(0);

                newFactors.add(factor);
            }
            current = current.plusDays(1);
        }

        // 批量插入
        if (!newFactors.isEmpty()) {
            this.saveBatch(newFactors);
        }

        int totalDays = (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        return String.format("成功生成%d条记录，跳过%d条已存在记录",
                newFactors.size(), existingDates.size());
    }

    @Override
    public void updateFactor(CalendarUpdateDTO dto) {
        CalendarFactor factor = this.getById(dto.getId());
        if (factor == null) {
            throw new RuntimeException("日历因子不存在");
        }

        // 更新字段
        if (dto.getIsHoliday() != null) {
            factor.setIsHoliday(dto.getIsHoliday());
        }
        if (dto.getHolidayName() != null) {
            factor.setHolidayName(dto.getHolidayName());
        }
        if (dto.getWeather() != null) {
            factor.setWeather(dto.getWeather());
        }

        this.updateById(factor);
    }

    /**
     * 转换为VO
     */
    private CalendarFactorVO convertToVO(CalendarFactor entity) {
        CalendarFactorVO vo = new CalendarFactorVO();
        BeanUtils.copyProperties(entity, vo);

        // 星期文本
        if (entity.getDayOfWeek() != null) {
            String[] weekDays = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
            vo.setDayOfWeekText(weekDays[entity.getDayOfWeek()]);
        }

        // 天气图标
        vo.setWeatherIcon(getWeatherIcon(entity.getWeather()));

        return vo;
    }

    /**
     * 获取天气图标
     */
    private String getWeatherIcon(String weather) {
        if (weather == null) return null;

        return switch (weather) {
            case "晴" -> "☀️";
            case "多云" -> "☁️";
            case "小雨", "大雨" -> "🌧️";
            case "雪" -> "❄️";
            case "炎热" -> "🌡️";
            default -> null;
        };
    }
}