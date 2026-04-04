package com.saul.dashboard.controller;

import com.saul.common.Result;
import com.saul.dashboard.service.IDashboardService;
import com.saul.dashboard.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页控制台 Controller
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final IDashboardService dashboardService;

    /**
     * 获取首页控制台聚合数据
     */
    @GetMapping
    public Result<DashboardVO> getDashboard() {
        DashboardVO data = dashboardService.getDashboardData();
        return Result.success(data);
    }
}