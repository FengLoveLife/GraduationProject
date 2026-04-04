package com.saul.dashboard.service;

import com.saul.dashboard.vo.DashboardVO;

/**
 * 首页控制台 Service 接口
 */
public interface IDashboardService {

    /**
     * 获取首页控制台聚合数据
     * @return 聚合数据
     */
    DashboardVO getDashboardData();
}