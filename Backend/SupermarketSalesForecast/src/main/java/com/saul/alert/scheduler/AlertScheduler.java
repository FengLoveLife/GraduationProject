package com.saul.alert.scheduler;

import com.saul.alert.service.IAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 预警定时任务
 * 每天 0 点自动执行全量库存巡检与节假日预警。
 * 测试时直接调用 POST /api/alert/trigger 接口，无需等到 0 点。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertScheduler {

    private final IAlertService alertService;

    @Scheduled(cron = "0 0 0 * * ?")
    public void dailyAlertCheck() {
        log.info("[定时预警] 每日 0 点巡检启动");
        try {
            alertService.checkAllAlerts();
        } catch (Exception e) {
            log.error("[定时预警] 巡检异常", e);
        }
    }
}
