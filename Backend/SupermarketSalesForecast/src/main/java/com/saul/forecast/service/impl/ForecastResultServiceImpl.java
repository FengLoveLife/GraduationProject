package com.saul.forecast.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.saul.forecast.dto.ForecastQueryDTO;
import com.saul.forecast.dto.ForecastRunDTO;
import com.saul.forecast.entity.ForecastResult;
import com.saul.forecast.mapper.ForecastResultMapper;
import com.saul.forecast.service.IForecastResultService;
import com.saul.forecast.vo.ForecastCompareVO;
import com.saul.forecast.vo.ForecastResultVO;
import com.saul.product.entity.Product;
import com.saul.product.mapper.ProductMapper;
import com.saul.sales.mapper.SalesOrderItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 销量预测结果 Service 实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ForecastResultServiceImpl extends ServiceImpl<ForecastResultMapper, ForecastResult>
        implements IForecastResultService {

    private final ProductMapper productMapper;
    private final RestTemplate restTemplate;
    private final SalesOrderItemMapper salesOrderItemMapper;

    @Value("${forecast.python-service-url:http://localhost:8000}")
    private String pythonServiceUrl;

    @Override
    public List<ForecastResultVO> getForecastResults(ForecastQueryDTO queryDTO) {
        // 构建查询条件
        LambdaQueryWrapper<ForecastResult> wrapper = new LambdaQueryWrapper<>();

        // 预测日期筛选
        if (queryDTO.getForecastDate() != null) {
            if (queryDTO.getDays() != null && queryDTO.getDays() > 1) {
                LocalDate endDate = queryDTO.getForecastDate().plusDays(queryDTO.getDays() - 1);
                wrapper.between(ForecastResult::getForecastDate, queryDTO.getForecastDate(), endDate);
            } else {
                wrapper.eq(ForecastResult::getForecastDate, queryDTO.getForecastDate());
            }
        }

        // 商品 ID 筛选
        if (queryDTO.getProductId() != null) {
            wrapper.eq(ForecastResult::getProductId, queryDTO.getProductId());
        }

        // 分类 ID 筛选
        if (queryDTO.getCategoryId() != null) {
            wrapper.eq(ForecastResult::getCategoryId, queryDTO.getCategoryId());
        }

        // 商品名称模糊搜索
        if (queryDTO.getProductName() != null && !queryDTO.getProductName().isEmpty()) {
            wrapper.like(ForecastResult::getProductName, queryDTO.getProductName());
        }

        // 排序
        wrapper.orderByAsc(ForecastResult::getCategoryName)
                .orderByAsc(ForecastResult::getProductName)
                .orderByAsc(ForecastResult::getForecastDate);

        // 查询预测结果
        List<ForecastResult> results = this.list(wrapper);

        // 转换为 VO 并补充库存信息
        return convertToVOList(results, queryDTO.getStockStatus());
    }

    @Override
    public Map<String, Object> runBatchForecast(ForecastRunDTO dto) {
        log.info("触发批量预测：startDate={}, days={}", dto.getForecastStart(), dto.getForecastDays());

        try {
            String url = pythonServiceUrl + "/forecast/run";
            if (dto.getForecastStart() != null) {
                url += "?forecast_start=" + dto.getForecastStart() + "&forecast_days=" + dto.getForecastDays();
            } else {
                url += "?forecast_days=" + dto.getForecastDays();
            }

            ResponseEntity<Map> response = restTemplate.postForEntity(url, null, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = response.getBody();
                log.info("批量预测完成：{}", result);
                return result;
            } else {
                throw new RuntimeException("Python 预测服务返回异常：" + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("调用 Python 预测服务失败", e);
            throw new RuntimeException("调用 Python 预测服务失败：" + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getForecastServiceStatus() {
        try {
            String url = pythonServiceUrl + "/forecast/status";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "预测服务返回异常");
                return error;
            }

        } catch (Exception e) {
            log.error("查询预测服务状态失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "offline");
            error.put("message", "无法连接预测服务：" + e.getMessage());
            return error;
        }
    }

    @Override
    public Map<String, Object> getForecastSummary(String forecastDate) {
        Map<String, Object> summary = new HashMap<>();

        // 如果没有传入日期，默认查询"今天"的预测数据
        if (forecastDate == null) {
            forecastDate = LocalDate.now().toString();
        }

        LambdaQueryWrapper<ForecastResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ForecastResult::getForecastDate, forecastDate);

        List<ForecastResult> results = this.list(wrapper);

        int totalProducts = results.size();
        int totalPredicted = results.stream().mapToInt(r -> r.getPredictedQuantity() != null ? r.getPredictedQuantity() : 0).sum();

        // 计算预测总销售额 = Σ(预测销量 × 商品零售价)
        java.math.BigDecimal totalPredictedAmount = java.math.BigDecimal.ZERO;

        // 收集所有商品ID，批量查询商品信息
        Set<Long> productIds = results.stream()
                .map(ForecastResult::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (!productIds.isEmpty()) {
            // 批量查询商品信息获取售价
            List<Product> products = productMapper.selectBatchIds(productIds);
            Map<Long, java.math.BigDecimal> priceMap = products.stream()
                    .collect(Collectors.toMap(
                            Product::getId,
                            p -> p.getSalePrice() != null ? p.getSalePrice() : java.math.BigDecimal.ZERO,
                            (a, b) -> a
                    ));

            // 计算预测总销售额
            for (ForecastResult result : results) {
                if (result.getProductId() != null && result.getPredictedQuantity() != null) {
                    java.math.BigDecimal price = priceMap.getOrDefault(result.getProductId(), java.math.BigDecimal.ZERO);
                    totalPredictedAmount = totalPredictedAmount.add(price.multiply(new java.math.BigDecimal(result.getPredictedQuantity())));
                }
            }
        }

        Map<String, Integer> categoryCount = new HashMap<>();
        for (ForecastResult result : results) {
            String categoryName = result.getCategoryName() != null ? result.getCategoryName() : "未分类";
            categoryCount.merge(categoryName, 1, Integer::sum);
        }

        // 查询昨天的实际销售数据（预测日期的前一天）
        java.math.BigDecimal yesterdayTotalQuantity = java.math.BigDecimal.ZERO;
        java.math.BigDecimal yesterdayTotalAmount = java.math.BigDecimal.ZERO;
        java.math.BigDecimal quantityGrowthRate = null;
        java.math.BigDecimal amountGrowthRate = null;
        String compareDate = null;

        if (forecastDate != null) {
            try {
                LocalDate forecastLocalDate = LocalDate.parse(forecastDate);
                LocalDate yesterdayDate = forecastLocalDate.minusDays(1);
                compareDate = yesterdayDate.toString();

                Map<String, Object> yesterdaySales = salesOrderItemMapper.getDailySalesSummary(compareDate);
                if (yesterdaySales != null) {
                    Object qtyObj = yesterdaySales.get("totalQuantity");
                    Object amtObj = yesterdaySales.get("totalAmount");

                    if (qtyObj != null) {
                        yesterdayTotalQuantity = new java.math.BigDecimal(qtyObj.toString());
                    }
                    if (amtObj != null) {
                        yesterdayTotalAmount = new java.math.BigDecimal(amtObj.toString());
                    }

                    // 计算增长率：(预测值 - 昨日实际值) / 昨日实际值 * 100
                    if (yesterdayTotalQuantity.compareTo(java.math.BigDecimal.ZERO) > 0) {
                        quantityGrowthRate = new java.math.BigDecimal(totalPredicted)
                                .subtract(yesterdayTotalQuantity)
                                .divide(yesterdayTotalQuantity, 4, java.math.RoundingMode.HALF_UP)
                                .multiply(new java.math.BigDecimal(100))
                                .setScale(1, java.math.RoundingMode.HALF_UP);
                    }
                    if (yesterdayTotalAmount.compareTo(java.math.BigDecimal.ZERO) > 0) {
                        amountGrowthRate = totalPredictedAmount
                                .subtract(yesterdayTotalAmount)
                                .divide(yesterdayTotalAmount, 4, java.math.RoundingMode.HALF_UP)
                                .multiply(new java.math.BigDecimal(100))
                                .setScale(1, java.math.RoundingMode.HALF_UP);
                    }
                }
            } catch (Exception e) {
                log.warn("获取昨日销售数据失败: {}", e.getMessage());
            }
        }

        summary.put("forecastDate", forecastDate);
        summary.put("totalProducts", totalProducts);
        summary.put("totalPredicted", totalPredicted);
        summary.put("totalPredictedAmount", totalPredictedAmount);
        summary.put("categoryCount", categoryCount);

        // 与昨日对比数据
        summary.put("yesterdayQuantity", yesterdayTotalQuantity.intValue());
        summary.put("yesterdayAmount", yesterdayTotalAmount);
        summary.put("quantityGrowthRate", quantityGrowthRate);
        summary.put("amountGrowthRate", amountGrowthRate);

        return summary;
    }

    @Override
    public List<ForecastCompareVO> getForecastVsActual(String startDate, String endDate, Long productId, Long categoryId) {
        log.info("查询预测 vs 实际对比数据：startDate={}, endDate={}, productId={}, categoryId={}",
                startDate, endDate, productId, categoryId);

        // 1. 查询预测数据
        LambdaQueryWrapper<ForecastResult> forecastWrapper = new LambdaQueryWrapper<>();
        forecastWrapper.between(ForecastResult::getForecastDate,
                LocalDate.parse(startDate),
                LocalDate.parse(endDate));

        if (productId != null) {
            forecastWrapper.eq(ForecastResult::getProductId, productId);
        }
        if (categoryId != null) {
            forecastWrapper.eq(ForecastResult::getCategoryId, categoryId);
        }

        List<ForecastResult> forecastResults = this.list(forecastWrapper);

        // 2. 查询实际销售数据
        List<Map<String, Object>> salesData = salesOrderItemMapper.getActualSalesByDate(startDate, endDate, productId, categoryId);

        // 3. 构建实际销量映射：(date, productId) -> actualQuantity
        Map<String, Integer> salesMap = new HashMap<>();
        for (Map<String, Object> row : salesData) {
            String date = row.get("saleDate").toString();
            Long pid = ((Number) row.get("productId")).longValue();
            Integer qty = ((Number) row.get("actualQuantity")).intValue();
            salesMap.put(date + "_" + pid, qty);
        }

        // 4. 合并数据，计算差异和误差率
        List<ForecastCompareVO> result = new ArrayList<>();
        for (ForecastResult forecast : forecastResults) {
            ForecastCompareVO vo = new ForecastCompareVO();
            vo.setDate(forecast.getForecastDate().toString());
            vo.setProductId(forecast.getProductId());
            vo.setProductCode(forecast.getProductCode());
            vo.setProductName(forecast.getProductName());
            vo.setCategoryId(forecast.getCategoryId());
            vo.setCategoryName(forecast.getCategoryName());
            vo.setPredictedQuantity(forecast.getPredictedQuantity());

            String key = forecast.getForecastDate().toString() + "_" + forecast.getProductId();
            Integer actualQty = salesMap.getOrDefault(key, 0);
            vo.setActualQuantity(actualQty);

            int variance = forecast.getPredictedQuantity() - actualQty;
            vo.setVariance(variance);

            double errorRate = 0;
            if (actualQty > 0) {
                errorRate = Math.abs(variance) * 100.0 / actualQty;
            }
            vo.setErrorRate(Math.round(errorRate * 100.0) / 100.0);

            result.add(vo);
        }

        result.sort(Comparator
                .comparing((ForecastCompareVO v) -> v.getDate())
                .thenComparing(ForecastCompareVO::getProductId));

        return result;
    }

    @Override
    public List<Map<String, Object>> getForecastTrend(String startDate, String endDate) {
        // 1. 查询预测数据，按日期汇总
        List<ForecastResult> forecastResults = this.list(
            new LambdaQueryWrapper<ForecastResult>()
                .between(ForecastResult::getForecastDate, LocalDate.parse(startDate), LocalDate.parse(endDate))
        );

        // 按日期汇总预测销量
        Map<LocalDate, Integer> forecastMap = forecastResults.stream()
            .collect(Collectors.groupingBy(
                ForecastResult::getForecastDate,
                Collectors.summingInt(r -> r.getPredictedQuantity() != null ? r.getPredictedQuantity() : 0)
            ));

        // 2. 查询实际销售数据，按日期汇总
        List<Map<String, Object>> salesSum = salesOrderItemMapper.getActualSalesSumByDate(startDate, endDate);

        // 按日期汇总实际销量
        Map<String, Integer> salesMap = new HashMap<>();
        if (salesSum != null) {
            for (Map<String, Object> row : salesSum) {
                Object dateObj = row.get("saleDate");
                Object qtyObj = row.get("actualQuantity");
                if (dateObj != null && qtyObj != null) {
                    salesMap.put(dateObj.toString(), ((Number) qtyObj).intValue());
                }
            }
        }

        // 3. 合并数据，生成结果
        List<Map<String, Object>> result = new ArrayList<>();
        Set<LocalDate> allDates = new HashSet<>();
        allDates.addAll(forecastMap.keySet());

        for (String dateStr : salesMap.keySet()) {
            try {
                allDates.add(LocalDate.parse(dateStr));
            } catch (Exception e) {
                // 忽略无效日期
            }
        }

        for (LocalDate date : allDates) {
            Map<String, Object> data = new HashMap<>();
            data.put("date", date.toString());
            data.put("predictedQuantity", forecastMap.getOrDefault(date, 0));
            data.put("actualQuantity", salesMap.getOrDefault(date.toString(), 0));
            result.add(data);
        }

        // 4. 按日期排序
        result.sort(Comparator.comparing(r -> r.get("date").toString()));

        return result;
    }

    /**
     * 查询预测数据按日期汇总
     */
    private List<Map<String, Object>> getForecastSumByDate(String startDate, String endDate) {
        List<ForecastResult> results = this.list(
            new LambdaQueryWrapper<ForecastResult>()
                .between(ForecastResult::getForecastDate, LocalDate.parse(startDate), LocalDate.parse(endDate))
        );

        Map<LocalDate, Integer> sumMap = results.stream()
            .collect(Collectors.groupingBy(
                ForecastResult::getForecastDate,
                Collectors.summingInt(r -> r.getPredictedQuantity() != null ? r.getPredictedQuantity() : 0)
            ));

        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Map.Entry<LocalDate, Integer> entry : sumMap.entrySet()) {
            Map<String, Object> m = new HashMap<>();
            m.put("date", entry.getKey().toString());
            m.put("predictedQuantity", entry.getValue());
            resultList.add(m);
        }
        return resultList;
    }

    private List<ForecastResultVO> convertToVOList(List<ForecastResult> results, String stockStatusFilter) {
        List<ForecastResultVO> voList = new ArrayList<>();

        Set<Long> productIds = new HashSet<>();
        for (ForecastResult result : results) {
            if (result.getProductId() != null) {
                productIds.add(result.getProductId());
            }
        }

        Map<Long, Product> productMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            List<Product> products = productMapper.selectBatchIds(productIds);
            for (Product product : products) {
                productMap.put(product.getId(), product);
            }
        }

        for (ForecastResult result : results) {
            ForecastResultVO vo = new ForecastResultVO();
            vo.setId(result.getId());
            vo.setForecastDate(result.getForecastDate());
            vo.setProductId(result.getProductId());
            vo.setProductCode(result.getProductCode());
            vo.setProductName(result.getProductName());
            vo.setCategoryId(result.getCategoryId());
            vo.setCategoryName(result.getCategoryName());
            vo.setPredictedQuantity(result.getPredictedQuantity());

            Product product = productMap.get(result.getProductId());
            if (product != null) {
                vo.setCurrentStock(product.getStock());
                vo.setSafetyStock(product.getSafetyStock());

                int suggestedPurchase = Math.max(0,
                        result.getPredictedQuantity() + product.getSafetyStock() - product.getStock());
                vo.setSuggestedPurchase(suggestedPurchase);

                String stockStatus = calculateStockStatus(product.getStock(), product.getSafetyStock(),
                        result.getPredictedQuantity());
                vo.setStockStatus(stockStatus);
            } else {
                vo.setCurrentStock(0);
                vo.setSafetyStock(0);
                vo.setSuggestedPurchase(0);
                vo.setStockStatus("unknown");
            }

            if (stockStatusFilter != null && !stockStatusFilter.isEmpty()) {
                if (!stockStatusFilter.equals(vo.getStockStatus())) {
                    continue;
                }
            }

            voList.add(vo);
        }

        return voList;
    }

    private String calculateStockStatus(Integer currentStock, Integer safetyStock, Integer predictedQuantity) {
        if (currentStock == null) currentStock = 0;
        if (safetyStock == null) safetyStock = 0;
        if (predictedQuantity == null) predictedQuantity = 0;

        // 先判断最危急的：库存已低于安全库存
        if (currentStock < safetyStock) {
            return "warning";
        }

        // 再判断：库存不足以覆盖预测销量+安全库存
        if (currentStock < predictedQuantity + safetyStock) {
            return "needPurchase";
        }

        return "sufficient";
    }
}
