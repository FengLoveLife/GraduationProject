package com.saul.alert.controller;

import com.saul.alert.entity.AlertLog;
import com.saul.alert.service.IAlertService;
import com.saul.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 预警通知 Controller
 */
@RestController
@RequestMapping("/api/alert")
@RequiredArgsConstructor
public class AlertController {

    private final IAlertService alertService;

    /**
     * 查询未读通知列表及数量
     * 前端 Header 铃铛轮询此接口
     */
    @GetMapping("/unread")
    public Result<Map<String, Object>> getUnread() {
        return Result.success(alertService.getUnreadAlerts());
    }

    /**
     * 全部标为已读
     * 管理员打开通知面板时调用
     */
    @PutMapping("/read-all")
    public Result<String> readAll() {
        alertService.markAllRead();
        return Result.success("已全部标为已读", "ok");
    }

    /**
     * 查询近7天（含今天）的全部预警记录
     */
    @GetMapping("/history")
    public Result<List<AlertLog>> history() {
        return Result.success(alertService.getRecentAlerts());
    }

    /**
     * 手动触发全量预警巡检
     * 等同于定时任务的0点逻辑，用于测试和演示
     */
    @PostMapping("/trigger")
    public Result<Map<String, Object>> trigger() {
        Map<String, Object> result = alertService.checkAllAlerts();
        return Result.success("预警巡检完成", result);
    }
}
