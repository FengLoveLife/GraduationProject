package com.saul.restocking.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.saul.restocking.entity.PurchaseSuggestion;
import com.saul.restocking.mapper.PurchaseSuggestionMapper;
import com.saul.restocking.service.IPurchaseSuggestionService;
import com.saul.restocking.vo.PurchaseSuggestionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 进货建议服务实现（核心逻辑）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseSuggestionServiceImpl extends ServiceImpl<PurchaseSuggestionMapper, PurchaseSuggestion>
        implements IPurchaseSuggestionService {

    private final PurchaseSuggestionMapper suggestionMapper;

    /**
     * 生成进货建议（核心方法）
     */
    @Override
    @Transactional
    public Map<String, Object> generateSuggestions() {
        log.info("开始生成进货建议...");

        // Step 1: 清空待处理的旧建议
        LambdaQueryWrapper<PurchaseSuggestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseSuggestion::getStatus, 0);
        this.remove(wrapper);

        // Step 2: 批量预加载销量数据，消除N+1查询
        // 历史日均：过去30天
        Map<Long, BigDecimal> historicalDailyMap = buildHistoricalDailyMap(30);
        // 预测日均：未来7天
        Map<Long, BigDecimal> predictedDailyMap = buildPredictedDailyMap(7);
        log.info("批量加载完成：历史{}个商品，预测{}个商品", historicalDailyMap.size(), predictedDailyMap.size());

        // Step 3: 扫描红灯商品（库存 <= 安全库存）
        List<Map<String, Object>> redLightProducts = suggestionMapper.getRedLightProducts();
        log.info("发现红灯商品: {} 个", redLightProducts.size());

        List<PurchaseSuggestion> allSuggestions = new ArrayList<>();

        for (Map<String, Object> product : redLightProducts) {
            PurchaseSuggestion suggestion = createSuggestion(product, 1, historicalDailyMap, predictedDailyMap);
            allSuggestions.add(suggestion);
        }

        // Step 4: 独立扫描所有非红灯商品，检查黄灯
        List<Map<String, Object>> nonRedProducts = suggestionMapper.getNonRedLightProducts();
        for (Map<String, Object> product : nonRedProducts) {
            Long productId = ((Number) product.get("productId")).longValue();
            Integer currentStock = ((Number) product.get("currentStock")).intValue();
            Integer safetyStock = ((Number) product.get("safetyStock")).intValue();
            Integer restockCycleDays = product.get("restockCycleDays") != null
                    ? ((Number) product.get("restockCycleDays")).intValue() : 7;

            BigDecimal dailySales = blendDailySales(productId, restockCycleDays, historicalDailyMap, predictedDailyMap);

            // 黄灯线 = 安全库存 + 周期销量 × 30%
            BigDecimal yellowLine = BigDecimal.valueOf(safetyStock)
                    .add(dailySales.multiply(BigDecimal.valueOf(restockCycleDays))
                            .multiply(BigDecimal.valueOf(0.3)));

            if (BigDecimal.valueOf(currentStock).compareTo(yellowLine) <= 0) {
                PurchaseSuggestion suggestion = createSuggestion(product, 2, historicalDailyMap, predictedDailyMap);
                allSuggestions.add(suggestion);
            }
        }

        // Step 5: 统计结果
        int redCount = (int) allSuggestions.stream().filter(s -> s.getLightStatus() == 1).count();
        int yellowCount = (int) allSuggestions.stream().filter(s -> s.getLightStatus() == 2).count();

        // Step 6: 批量保存
        if (!allSuggestions.isEmpty()) {
            this.saveBatch(allSuggestions);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("redCount", redCount);
        result.put("yellowCount", yellowCount);
        result.put("totalCount", allSuggestions.size());
        result.put("message", allSuggestions.isEmpty() ? "当前没有需要补货的商品" : "生成成功");
        log.info("进货建议生成完成：红灯 {} 个，黄灯 {} 个", redCount, yellowCount);

        return result;
    }

    /**
     * 批量构建历史日均销量 Map（productId → 日均）
     */
    private Map<Long, BigDecimal> buildHistoricalDailyMap(int days) {
        List<Map<String, Object>> rows = suggestionMapper.batchGetHistoricalSales(days);
        Map<Long, BigDecimal> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long productId = ((Number) row.get("productId")).longValue();
            BigDecimal total = row.get("totalQuantity") != null
                    ? new BigDecimal(row.get("totalQuantity").toString()) : BigDecimal.ZERO;
            Integer salesDays = row.get("salesDays") != null
                    ? ((Number) row.get("salesDays")).intValue() : 0;
            if (total.compareTo(BigDecimal.ZERO) > 0 && salesDays > 0) {
                map.put(productId, total.divide(BigDecimal.valueOf(salesDays), 2, RoundingMode.HALF_UP));
            }
        }
        return map;
    }

    /**
     * 批量构建预测日均销量 Map（productId → 日均）
     */
    private Map<Long, BigDecimal> buildPredictedDailyMap(int days) {
        List<Map<String, Object>> rows = suggestionMapper.batchGetPredictedSales(days);
        Map<Long, BigDecimal> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long productId = ((Number) row.get("productId")).longValue();
            BigDecimal total = row.get("totalPredicted") != null
                    ? new BigDecimal(row.get("totalPredicted").toString()) : BigDecimal.ZERO;
            Integer forecastDays = row.get("forecastDays") != null
                    ? ((Number) row.get("forecastDays")).intValue() : 0;
            if (total.compareTo(BigDecimal.ZERO) > 0 && forecastDays > 0) {
                map.put(productId, total.divide(BigDecimal.valueOf(forecastDays), 2, RoundingMode.HALF_UP));
            }
        }
        return map;
    }

    /**
     * 三档加权公式（从预加载Map取值，无DB调用）
     *
     * - ≤7天：100% AI预测
     * - 7-14天：60% 预测 + 40% 历史
     * - ≥15天：30% 预测 + 70% 历史
     */
    private BigDecimal blendDailySales(Long productId, Integer restockCycleDays,
                                       Map<Long, BigDecimal> historicalMap,
                                       Map<Long, BigDecimal> predictedMap) {
        BigDecimal hist = historicalMap.getOrDefault(productId, BigDecimal.ZERO);
        BigDecimal pred = predictedMap.getOrDefault(productId, BigDecimal.ZERO);

        if (restockCycleDays <= 7) {
            return pred.compareTo(BigDecimal.ZERO) > 0 ? pred : hist;
        }
        if (restockCycleDays < 15) {
            if (pred.compareTo(BigDecimal.ZERO) == 0) return hist;
            return pred.multiply(BigDecimal.valueOf(0.6))
                    .add(hist.multiply(BigDecimal.valueOf(0.4)))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        // ≥15天
        if (pred.compareTo(BigDecimal.ZERO) == 0) return hist;
        return pred.multiply(BigDecimal.valueOf(0.3))
                .add(hist.multiply(BigDecimal.valueOf(0.7)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 创建进货建议记录（使用预加载Map，无额外DB调用）
     */
    private PurchaseSuggestion createSuggestion(Map<String, Object> product, Integer lightStatus,
                                                 Map<Long, BigDecimal> historicalMap,
                                                 Map<Long, BigDecimal> predictedMap) {
        PurchaseSuggestion suggestion = new PurchaseSuggestion();

        Long productId = ((Number) product.get("productId")).longValue();
        suggestion.setProductId(productId);
        suggestion.setProductCode((String) product.get("productCode"));
        suggestion.setProductName((String) product.get("productName"));
        suggestion.setCategoryId(((Number) product.get("categoryId")).longValue());
        suggestion.setCategoryName((String) product.get("categoryName"));

        Object purchasePrice = product.get("purchasePrice");
        if (purchasePrice != null) {
            suggestion.setPurchasePrice(new BigDecimal(purchasePrice.toString()));
        }

        Integer currentStock = ((Number) product.get("currentStock")).intValue();
        Integer safetyStock = ((Number) product.get("safetyStock")).intValue();
        Integer restockCycleDays = product.get("restockCycleDays") != null
                ? ((Number) product.get("restockCycleDays")).intValue() : 7;

        suggestion.setCurrentStock(currentStock);
        suggestion.setSafetyStock(safetyStock);
        suggestion.setLightStatus(lightStatus);

        BigDecimal dailySales = blendDailySales(productId, restockCycleDays, historicalMap, predictedMap);
        suggestion.setDailySales(dailySales);

        // 目标库存 = 日均销量 × 补货周期 + 安全库存
        int targetStock = dailySales.multiply(BigDecimal.valueOf(restockCycleDays))
                .add(BigDecimal.valueOf(safetyStock))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
        suggestion.setTargetStock(targetStock);

        int predictedQuantity = dailySales.multiply(BigDecimal.valueOf(restockCycleDays))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
        suggestion.setPredictedQuantity(predictedQuantity);

        int suggestedQuantity = Math.max(0, targetStock - currentStock);
        suggestion.setSuggestedQuantity(suggestedQuantity);
        suggestion.setFinalQuantity(suggestedQuantity);

        suggestion.setStatus(0);
        suggestion.setCreateTime(LocalDateTime.now());
        suggestion.setUpdateTime(LocalDateTime.now());

        return suggestion;
    }

    @Override
    public List<PurchaseSuggestionVO> getSuggestionList(Integer status, Integer lightStatus, Long categoryId) {
        LambdaQueryWrapper<PurchaseSuggestion> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(PurchaseSuggestion::getStatus, status);
        }
        if (lightStatus != null) {
            wrapper.eq(PurchaseSuggestion::getLightStatus, lightStatus);
        }
        if (categoryId != null) {
            wrapper.eq(PurchaseSuggestion::getCategoryId, categoryId);
        }

        wrapper.orderByAsc(PurchaseSuggestion::getLightStatus) // 红灯优先
                .orderByAsc(PurchaseSuggestion::getCategoryName)
                .orderByDesc(PurchaseSuggestion::getSuggestedQuantity);

        List<PurchaseSuggestion> suggestions = this.list(wrapper);
        return convertToVOList(suggestions);
    }

    @Override
    public Map<String, Object> getSuggestionSummary() {
        Map<String, Object> summary = new HashMap<>();

        LambdaQueryWrapper<PurchaseSuggestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseSuggestion::getStatus, 0); // 只统计待处理的

        List<PurchaseSuggestion> suggestions = this.list(wrapper);

        int redCount = (int) suggestions.stream().filter(s -> s.getLightStatus() == 1).count();
        int yellowCount = (int) suggestions.stream().filter(s -> s.getLightStatus() == 2).count();
        int totalQuantity = suggestions.stream()
                .mapToInt(s -> s.getSuggestedQuantity() != null ? s.getSuggestedQuantity() : 0)
                .sum();

        // 按分类统计
        Map<String, Integer> categoryCount = new HashMap<>();
        for (PurchaseSuggestion s : suggestions) {
            String categoryName = s.getCategoryName() != null ? s.getCategoryName() : "未分类";
            categoryCount.merge(categoryName, 1, Integer::sum);
        }

        summary.put("totalCount", suggestions.size());
        summary.put("redCount", redCount);
        summary.put("yellowCount", yellowCount);
        summary.put("totalQuantity", totalQuantity);
        summary.put("categoryCount", categoryCount);

        return summary;
    }

    @Override
    public boolean adjustQuantity(Long id, Integer adjustedQuantity) {
        PurchaseSuggestion suggestion = this.getById(id);
        if (suggestion == null || suggestion.getStatus() != 0) {
            return false;
        }

        suggestion.setAdjustedQuantity(adjustedQuantity);
        suggestion.setFinalQuantity(adjustedQuantity);
        suggestion.setUpdateTime(LocalDateTime.now());

        return this.updateById(suggestion);
    }

    @Override
    public boolean ignoreSuggestion(Long id) {
        PurchaseSuggestion suggestion = this.getById(id);
        if (suggestion == null || suggestion.getStatus() != 0) {
            return false;
        }

        suggestion.setStatus(2); // 已忽略
        suggestion.setUpdateTime(LocalDateTime.now());

        return this.updateById(suggestion);
    }

    /**
     * 转换为VO列表
     */
    private List<PurchaseSuggestionVO> convertToVOList(List<PurchaseSuggestion> suggestions) {
        return suggestions.stream().map(s -> {
            PurchaseSuggestionVO vo = new PurchaseSuggestionVO();
            vo.setId(s.getId());
            vo.setProductId(s.getProductId());
            vo.setProductCode(s.getProductCode());
            vo.setProductName(s.getProductName());
            vo.setCategoryId(s.getCategoryId());
            vo.setCategoryName(s.getCategoryName());
            vo.setPurchasePrice(s.getPurchasePrice());
            vo.setPredictedQuantity(s.getPredictedQuantity());
            vo.setDailySales(s.getDailySales());
            vo.setCurrentStock(s.getCurrentStock());
            vo.setSafetyStock(s.getSafetyStock());
            vo.setTargetStock(s.getTargetStock());
            vo.setLightStatus(s.getLightStatus());
            vo.setLightStatusText(s.getLightStatus() == 1 ? "红灯" : "黄灯");
            vo.setSuggestedQuantity(s.getSuggestedQuantity());
            vo.setAdjustedQuantity(s.getAdjustedQuantity());
            vo.setFinalQuantity(s.getFinalQuantity());
            vo.setStatus(s.getStatus());
            vo.setStatusText(getStatusText(s.getStatus()));
            vo.setRemark(s.getRemark());
            vo.setCreateTime(s.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待处理";
            case 1: return "已生成进货单";
            case 2: return "已忽略";
            default: return "未知";
        }
    }
}