package com.saul.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.saul.dashboard.service.IDashboardService;
import com.saul.dashboard.vo.DashboardVO;
import com.saul.forecast.entity.ForecastResult;
import com.saul.forecast.mapper.ForecastResultMapper;
import com.saul.product.entity.Product;
import com.saul.product.mapper.ProductMapper;
import com.saul.restocking.entity.PurchaseOrder;
import com.saul.restocking.mapper.PurchaseOrderMapper;
import com.saul.sales.mapper.SalesOrderItemMapper;
import com.saul.sales.mapper.SalesOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 首页控制台 Service 实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements IDashboardService {

    private final SalesOrderItemMapper salesOrderItemMapper;
    private final SalesOrderMapper salesOrderMapper;
    private final ProductMapper productMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final ForecastResultMapper forecastResultMapper;

    @Override
    public DashboardVO getDashboardData() {
        DashboardVO vo = new DashboardVO();

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        String todayStr = today.toString();
        String yesterdayStr = yesterday.toString();

        // ========== 1. 今日销售额和订单数 ==========

        Map<String, Object> todaySalesData = salesOrderItemMapper.getDailySalesSummary(todayStr);
        BigDecimal todaySalesAmount = getBigDecimal(todaySalesData, "totalAmount");
        // 订单数从 sales_order 表查询（修复：之前错误地把销售商品总数当作订单数）
        Integer todayOrderCount = salesOrderMapper.getDailyOrderCount(todayStr);

        // 昨日数据（用于计算增长率）
        Map<String, Object> yesterdaySalesData = salesOrderItemMapper.getDailySalesSummary(yesterdayStr);
        BigDecimal yesterdaySalesAmount = getBigDecimal(yesterdaySalesData, "totalAmount");
        Integer yesterdayOrderCount = salesOrderMapper.getDailyOrderCount(yesterdayStr);

        // 计算增长率
        BigDecimal salesGrowth = calculateGrowthRate(todaySalesAmount, yesterdaySalesAmount);
        BigDecimal orderGrowth = calculateGrowthRate(new BigDecimal(todayOrderCount), new BigDecimal(yesterdayOrderCount));

        vo.setTodaySales(todaySalesAmount);
        vo.setSalesGrowth(salesGrowth);
        vo.setTodayOrders(todayOrderCount);
        vo.setOrderGrowth(orderGrowth);

        // ========== 2. 库存告急预警数 ==========

        // 库存告急：库存 <= 安全库存
        Long stockWarningCount = productMapper.selectCount(
            new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1)
                .apply("stock <= safety_stock")
        );
        vo.setStockWarning(stockWarningCount.intValue());

        // ========== 3. 待处理进货单数 ==========

        // 待处理进货单：待确认(0) + 已下单货在途(1)，均需用户关注
        Long pendingOrderCount = purchaseOrderMapper.selectCount(
            new LambdaQueryWrapper<PurchaseOrder>()
                .in(PurchaseOrder::getStatus, 0, 1)
        );
        vo.setPendingPurchase(pendingOrderCount.intValue());

        // ========== 4. 近7天销售趋势 + 预测 ==========

        List<DashboardVO.TrendDataVO> trendList = buildSalesTrend(today);
        vo.setSalesTrend(trendList);

        // ========== 5. 今日品类销量占比 ==========

        List<DashboardVO.CategoryPieVO> pieList = buildCategoryPie(todayStr);
        vo.setCategoryPie(pieList);

        return vo;
    }

    /**
     * 构建近7天销售趋势数据（历史 + 预测）
     */
    private List<DashboardVO.TrendDataVO> buildSalesTrend(LocalDate today) {
        List<DashboardVO.TrendDataVO> list = new ArrayList<>();

        // 历史数据：前4天
        LocalDate historyStart = today.minusDays(4);
        String historyStartStr = historyStart.toString();
        String todayStr = today.toString();

        // 查询历史销售数据（按日期汇总销售额）
        List<Map<String, Object>> historySales = salesOrderItemMapper.getActualSalesSumByDate(historyStartStr, todayStr);
        Map<String, BigDecimal> historyMap = new HashMap<>();
        for (Map<String, Object> row : historySales) {
            String date = row.get("saleDate").toString();
            // 计算销售额：需要再查一次获取 amount
            Map<String, Object> summary = salesOrderItemMapper.getDailySalesSummary(date);
            historyMap.put(date, getBigDecimal(summary, "totalAmount"));
        }

        // 预测数据：历史5天 + 未来3天（全部查出，用于对比准确率）
        LocalDate predictStart = today.minusDays(4);
        LocalDate predictEnd = today.plusDays(3);

        List<ForecastResult> forecastResults = forecastResultMapper.selectList(
            new LambdaQueryWrapper<ForecastResult>()
                .between(ForecastResult::getForecastDate, predictStart, predictEnd)
        );

        // 按日期汇总预测销售额（预测销量 × 商品售价）
        Map<LocalDate, BigDecimal> predictMap = new HashMap<>();
        for (ForecastResult fr : forecastResults) {
            if (fr.getPredictedQuantity() == null || fr.getProductId() == null) continue;

            Product product = productMapper.selectById(fr.getProductId());
            if (product == null || product.getSalePrice() == null) continue;

            BigDecimal predictedAmount = product.getSalePrice()
                .multiply(new BigDecimal(fr.getPredictedQuantity()));

            predictMap.merge(fr.getForecastDate(), predictedAmount, BigDecimal::add);
        }

        // 组装趋势数据（共7天：4天历史 + 今天 + 3天预测）
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        // 前4天历史（同时携带预测值，供前端对比准确率）
        for (int i = 4; i >= 1; i--) {
            LocalDate date = today.minusDays(i);
            DashboardVO.TrendDataVO item = new DashboardVO.TrendDataVO();
            item.setDate(date.format(formatter));
            item.setActualAmount(historyMap.getOrDefault(date.toString(), BigDecimal.ZERO));
            item.setPredictedAmount(predictMap.getOrDefault(date, null));
            item.setIsPrediction(false);
            list.add(item);
        }

        // 今天（历史数据 + 预测值）
        DashboardVO.TrendDataVO todayItem = new DashboardVO.TrendDataVO();
        todayItem.setDate(today.format(formatter));
        todayItem.setActualAmount(historyMap.getOrDefault(todayStr, BigDecimal.ZERO));
        todayItem.setPredictedAmount(predictMap.getOrDefault(today, null));
        todayItem.setIsPrediction(false);
        list.add(todayItem);

        // 未来3天预测
        for (int i = 1; i <= 3; i++) {
            LocalDate date = today.plusDays(i);
            DashboardVO.TrendDataVO item = new DashboardVO.TrendDataVO();
            item.setDate(date.format(formatter));
            item.setActualAmount(null);
            item.setPredictedAmount(predictMap.getOrDefault(date, BigDecimal.ZERO));
            item.setIsPrediction(true);
            list.add(item);
        }

        return list;
    }

    /**
     * 构建品类销量占比饼图数据
     * 降级策略：今日无数据 → 昨日数据 → 近7天数据
     */
    private List<DashboardVO.CategoryPieVO> buildCategoryPie(String todayStr) {
        LocalDate today = LocalDate.parse(todayStr);

        // 今日数据
        List<Map<String, Object>> categoryData = salesOrderItemMapper.getCategoryQuantityByDate(todayStr);

        String label = "今日";
        if (categoryData == null || categoryData.isEmpty()) {
            // 降级1：昨日数据
            String yesterdayStr = today.minusDays(1).toString();
            categoryData = salesOrderItemMapper.getCategoryQuantityByDate(yesterdayStr);
            label = "昨日";
        }
        if (categoryData == null || categoryData.isEmpty()) {
            // 降级2：近7天数据
            String weekAgoStr = today.minusDays(7).toString();
            categoryData = salesOrderItemMapper.getCategoryQuantityByDateRange(weekAgoStr, todayStr);
            label = "近7天";
        }

        List<DashboardVO.CategoryPieVO> list = new ArrayList<>();
        for (Map<String, Object> row : categoryData) {
            DashboardVO.CategoryPieVO item = new DashboardVO.CategoryPieVO();
            item.setName(row.get("categoryName").toString());
            item.setValue(((Number) row.get("totalQuantity")).intValue());
            list.add(item);
        }

        // 将数据来源标签传到 VO，供前端饼图标题展示
        if (!list.isEmpty()) {
            list.get(0).setLabel(label);
        }

        return list;
    }

    /**
     * 计算增长率：(当前值 - 昨日值) / 昨日值 * 100
     */
    private BigDecimal calculateGrowthRate(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return current.subtract(previous)
            .divide(previous, 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal(100))
            .setScale(1, RoundingMode.HALF_UP);
    }

    /**
     * 从 Map 中获取 BigDecimal 值
     */
    private BigDecimal getBigDecimal(Map<String, Object> map, String key) {
        if (map == null || map.get(key) == null) {
            return BigDecimal.ZERO;
        }
        Object value = map.get(key);
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        return BigDecimal.ZERO;
    }

    /**
     * 从 Map 中获取 Integer 值
     */
    private Integer getInt(Map<String, Object> map, String key) {
        if (map == null || map.get(key) == null) {
            return 0;
        }
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }
}