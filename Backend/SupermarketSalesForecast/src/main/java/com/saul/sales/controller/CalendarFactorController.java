package com.saul.sales.controller;

import com.saul.common.Result;
import com.saul.sales.dto.CalendarInitDTO;
import com.saul.sales.dto.CalendarQueryDTO;
import com.saul.sales.dto.CalendarUpdateDTO;
import com.saul.sales.service.ICalendarFactorService;
import com.saul.sales.vo.CalendarFactorVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 日历因子管理 Controller
 */
@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarFactorController {

    private final ICalendarFactorService calendarFactorService;

    /**
     * 按月份查询日历数据
     */
    @GetMapping("/month")
    public Result<List<CalendarFactorVO>> getMonthData(
            @RequestParam Integer year,
            @RequestParam Integer month) {
        return Result.success(calendarFactorService.getMonthData(year, month));
    }

    /**
     * 初始化月份日历数据
     */
    @PostMapping("/init")
    public Result<String> initMonthCalendar(@Valid @RequestBody CalendarInitDTO dto) {
        String message = calendarFactorService.initMonthCalendar(dto);
        return Result.success(message);
    }

    /**
     * 更新日历因子
     */
    @PutMapping("/{id}")
    public Result<String> updateFactor(
            @PathVariable Long id,
            @RequestBody CalendarUpdateDTO dto) {
        dto.setId(id);
        calendarFactorService.updateFactor(dto);
        return Result.success("更新成功");
    }

    /**
     * 获取单条日历因子详情
     */
    @GetMapping("/{id}")
    public Result<CalendarFactorVO> getById(@PathVariable Long id) {
        CalendarFactorVO vo = calendarFactorService.getMonthData(2026, 1).stream()
                .filter(v -> v.getId().equals(id))
                .findFirst()
                .orElse(null);
        return Result.success(vo);
    }
}