package com.saul.alert.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.saul.alert.entity.AlertLog;

import java.util.List;
import java.util.Map;

public interface IAlertService extends IService<AlertLog> {

    /**
     * 全量巡检：库存告急 + 节假日需求预警。
     * 由定时任务每日0点调用，也可通过接口手动触发。
     *
     * @return 本次新增的预警数量统计
     */
    Map<String, Object> checkAllAlerts();

    /**
     * 查询未读通知列表及数量（供 Header 铃铛使用）
     */
    Map<String, Object> getUnreadAlerts();

    /**
     * 全部标为已读
     */
    void markAllRead();

    /**
     * 查询近7天（含今天）的全部预警记录，按时间降序
     */
    List<AlertLog> getRecentAlerts();
}
