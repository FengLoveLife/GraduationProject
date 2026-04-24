package com.saul.alert.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.saul.alert.entity.AlertLog;
import com.saul.alert.mapper.AlertLogMapper;
import com.saul.alert.service.IAlertService;
import com.saul.product.entity.Product;
import com.saul.product.mapper.ProductMapper;
import com.saul.restocking.mapper.PurchaseSuggestionMapper;
import com.saul.sales.entity.CalendarFactor;
import com.saul.sales.mapper.CalendarFactorMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertServiceImpl extends ServiceImpl<AlertLogMapper, AlertLog> implements IAlertService {

    private final AlertLogMapper alertLogMapper;
    private final PurchaseSuggestionMapper purchaseSuggestionMapper;
    private final ProductMapper productMapper;
    private final CalendarFactorMapper calendarFactorMapper;

    // ==================== 对外接口 ====================

    @Override
    public Map<String, Object> checkAllAlerts() {
        LocalDate today = LocalDate.now();
        log.info("[预警巡检] 开始，日期: {}", today);

        int stockCount   = doCheckStockAlerts(today);
        int holidayCount = doCheckHolidayAlerts(today);

        log.info("[预警巡检] 完成：库存告急新增 {} 条，节假日预警新增 {} 条", stockCount, holidayCount);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stockAlertCount",   stockCount);
        result.put("holidayAlertCount", holidayCount);
        result.put("totalNewCount",     stockCount + holidayCount);
        return result;
    }

    @Override
    public Map<String, Object> getUnreadAlerts() {
        List<AlertLog> list = this.list(
            new LambdaQueryWrapper<AlertLog>()
                .eq(AlertLog::getIsRead, 0)
                .orderByDesc(AlertLog::getCreateTime)
                .last("LIMIT 50")
        );

        Map<String, Object> result = new HashMap<>();
        result.put("count", list.size());
        result.put("list",  list);
        return result;
    }

    @Override
    public void markAllRead() {
        alertLogMapper.markAllRead();
    }

    @Override
    public List<AlertLog> getRecentAlerts() {
        LocalDate since = LocalDate.now().minusDays(6);
        return this.list(
            new LambdaQueryWrapper<AlertLog>()
                .ge(AlertLog::getBizDate, since)
                .orderByDesc(AlertLog::getCreateTime)
        );
    }

    // ==================== 库存告急巡检 ====================

    private int doCheckStockAlerts(LocalDate today) {
        // 复用进货建议模块已有的红灯查询：stock <= safety_stock
        List<Map<String, Object>> redProducts = purchaseSuggestionMapper.getRedLightProducts();
        int count = 0;
        for (Map<String, Object> p : redProducts) {
            Long   productId   = ((Number) p.get("productId")).longValue();
            String productName = (String)  p.get("productName");
            int    stock       = ((Number) p.get("currentStock")).intValue();
            int    safetyStock = ((Number) p.get("safetyStock")).intValue();

            String content = String.format(
                "商品「%s」当前库存 %d 件，低于安全库存 %d 件，请及时补货。",
                productName, stock, safetyStock
            );
            if (saveAlert(1, productId, productName, content, today)) count++;
        }
        return count;
    }

    // ==================== 节假日需求预警巡检 ====================

    private int doCheckHolidayAlerts(LocalDate today) {
        // 查未来 7 天内的节假日
        List<CalendarFactor> holidays = calendarFactorMapper.selectList(
            new LambdaQueryWrapper<CalendarFactor>()
                .gt(CalendarFactor::getDate, today)
                .le(CalendarFactor::getDate, today.plusDays(7))
                .eq(CalendarFactor::getIsHoliday, 1)
                .orderByAsc(CalendarFactor::getDate)
        );

        if (holidays.isEmpty()) {
            log.info("[预警巡检] 未来7天无节假日，跳过节假日预警");
            return 0;
        }

        List<LocalDate> holidayDates = holidays.stream()
            .map(CalendarFactor::getDate)
            .collect(Collectors.toList());

        // 取第一条的节假日名称作为预警标题
        String holidayName = holidays.get(0).getHolidayName();
        String dateRange   = holidayDates.get(0) + " 至 " + holidayDates.get(holidayDates.size() - 1);
        log.info("[预警巡检] 检测到节假日「{}」，日期范围: {}，共 {} 天",
            holidayName, dateRange, holidayDates.size());

        // 查询节假日期间各商品的预测总需求
        List<Map<String, Object>> demands = alertLogMapper.getHolidayDemandByDates(holidayDates);
        if (demands.isEmpty()) {
            log.warn("[预警巡检] 节假日期间无预测数据，请先运行销量预测");
            return 0;
        }

        // 批量加载商品库存（避免 N+1）
        Set<Long> productIds = demands.stream()
            .map(d -> ((Number) d.get("productId")).longValue())
            .collect(Collectors.toSet());
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds)
            .stream()
            .collect(Collectors.toMap(Product::getId, p -> p));

        int count = 0;
        for (Map<String, Object> d : demands) {
            Long   productId   = ((Number) d.get("productId")).longValue();
            String productName = (String)  d.get("productName");
            int    demandQty   = ((Number) d.get("totalDemand")).intValue();

            Product product = productMap.get(productId);
            if (product == null) continue;

            // 判断：当前库存 < 节假日期间预测总需求 + 安全库存
            int threshold = demandQty + product.getSafetyStock();
            if (product.getStock() < threshold) {
                int shortage = threshold - product.getStock();
                String content = String.format(
                    "「%s」（%s）即将到来，商品「%s」节假日预测需求 %d 件，当前库存仅 %d 件，建议提前备货至少 %d 件。",
                    holidayName, dateRange, productName,
                    demandQty, product.getStock(), shortage
                );
                if (saveAlert(2, productId, productName, content, today)) count++;
            }
        }
        return count;
    }

    // ==================== 内部工具 ====================

    /**
     * 写入一条预警通知。
     * 依赖唯一索引 uk_alert_dedup(alert_type, product_id, biz_date) 保证同天去重。
     * INSERT IGNORE 保证重复时静默跳过，不抛异常。
     *
     * @return true=新增成功，false=今天已存在相同预警（跳过）
     */
    private boolean saveAlert(int type, Long productId, String productName,
                              String content, LocalDate bizDate) {
        AlertLog record = new AlertLog();
        record.setAlertType(type);
        record.setProductId(productId);
        record.setProductName(productName);
        record.setAlertContent(content);
        record.setIsRead(0);
        record.setBizDate(bizDate);
        record.setCreateTime(LocalDateTime.now());
        return alertLogMapper.insertIgnore(record) > 0;
    }
}
