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

        // Step 2: 扫描红灯商品（库存 <= 安全库存）
        List<Map<String, Object>> redLightProducts = suggestionMapper.getRedLightProducts();
        log.info("发现红灯商品: {} 个", redLightProducts.size());

        if (redLightProducts.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("redCount", 0);
            result.put("yellowCount", 0);
            result.put("totalCount", 0);
            result.put("message", "当前没有需要补货的商品");
            return result;
        }

        // Step 3: 提取红灯商品的分类ID
        Set<Long> redCategoryIds = redLightProducts.stream()
                .map(p -> ((Number) p.get("categoryId")).longValue())
                .collect(Collectors.toSet());

        // Step 4: 检查每个分类下的黄灯商品
        List<PurchaseSuggestion> allSuggestions = new ArrayList<>();

        // 先处理红灯商品
        for (Map<String, Object> product : redLightProducts) {
            PurchaseSuggestion suggestion = createSuggestion(product, 1); // 1 = 红灯
            allSuggestions.add(suggestion);
        }

        // 再检查同分类的黄灯商品
        for (Long categoryId : redCategoryIds) {
            List<Map<String, Object>> categoryProducts = suggestionMapper.getProductsByCategory(categoryId);

            for (Map<String, Object> product : categoryProducts) {
                Long productId = ((Number) product.get("productId")).longValue();
                Integer currentStock = ((Number) product.get("currentStock")).intValue();
                Integer safetyStock = ((Number) product.get("safetyStock")).intValue();
                Integer restockCycleDays = product.get("restockCycleDays") != null
                        ? ((Number) product.get("restockCycleDays")).intValue() : 7;

                // 跳过已经是红灯的商品
                if (currentStock <= safetyStock) {
                    continue;
                }

                // 计算平滑日均销量
                BigDecimal dailySales = calculateSmoothedDailySales(productId, restockCycleDays);

                // 计算黄灯线 = 安全库存 + 周期销量 × 30%
                BigDecimal cycleSales = dailySales.multiply(BigDecimal.valueOf(restockCycleDays));
                BigDecimal yellowLine = BigDecimal.valueOf(safetyStock).add(cycleSales.multiply(BigDecimal.valueOf(0.3)));

                // 判断是否为黄灯
                if (BigDecimal.valueOf(currentStock).compareTo(yellowLine) <= 0) {
                    PurchaseSuggestion suggestion = createSuggestion(product, 2); // 2 = 黄灯
                    suggestion.setDailySales(dailySales);
                    suggestion.setTargetStock(dailySales.multiply(BigDecimal.valueOf(restockCycleDays))
                            .add(BigDecimal.valueOf(safetyStock)).intValue());
                    suggestion.setSuggestedQuantity(Math.max(0,
                            suggestion.getTargetStock() - currentStock));
                    suggestion.setFinalQuantity(suggestion.getSuggestedQuantity());
                    allSuggestions.add(suggestion);
                }
            }
        }

        // Step 5: 批量保存进货建议
        this.saveBatch(allSuggestions);

        // Step 6: 统计结果
        int redCount = (int) allSuggestions.stream().filter(s -> s.getLightStatus() == 1).count();
        int yellowCount = (int) allSuggestions.stream().filter(s -> s.getLightStatus() == 2).count();

        Map<String, Object> result = new HashMap<>();
        result.put("redCount", redCount);
        result.put("yellowCount", yellowCount);
        result.put("totalCount", allSuggestions.size());
        result.put("message", "生成成功");
        log.info("进货建议生成完成：红灯 {} 个，黄灯 {} 个", redCount, yellowCount);

        return result;
    }

    /**
     * 计算平滑日均销量（加权公式）
     */
    private BigDecimal calculateSmoothedDailySales(Long productId, Integer restockCycleDays) {
        // 短周期生鲜（≤7天）：完全相信AI预测
        if (restockCycleDays <= 7) {
            Integer predictedTotal = suggestionMapper.getPredictedSalesTotal(productId, 7);
            Integer predictedDays = suggestionMapper.getPredictedSalesDays(productId, 7);

            if (predictedTotal != null && predictedDays != null && predictedDays > 0) {
                return BigDecimal.valueOf(predictedTotal)
                        .divide(BigDecimal.valueOf(predictedDays), 2, RoundingMode.HALF_UP);
            }
            // 没有预测数据，降级用历史数据
            return getHistoricalDailySales(productId, 30);
        }

        // 长周期标品（14/30天）：历史70% + AI预测30%
        BigDecimal historicalDailySales = getHistoricalDailySales(productId, 30);
        BigDecimal predictedDailySales = getPredictedDailySales(productId, 7);

        // 如果预测数据不足，完全依赖历史
        if (predictedDailySales == null || predictedDailySales.compareTo(BigDecimal.ZERO) == 0) {
            return historicalDailySales;
        }

        // 加权计算：70% 历史 + 30% AI
        return historicalDailySales.multiply(BigDecimal.valueOf(0.7))
                .add(predictedDailySales.multiply(BigDecimal.valueOf(0.3)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 获取历史日均销量
     */
    private BigDecimal getHistoricalDailySales(Long productId, Integer days) {
        BigDecimal historicalTotal = suggestionMapper.getHistoricalSalesTotal(productId, days);
        Integer historicalDays = suggestionMapper.getHistoricalSalesDays(productId, days);

        if (historicalTotal == null || historicalTotal.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        if (historicalDays == null || historicalDays == 0) {
            // 没有销售记录，返回0
            return BigDecimal.ZERO;
        }

        return historicalTotal.divide(BigDecimal.valueOf(historicalDays), 2, RoundingMode.HALF_UP);
    }

    /**
     * 获取预测日均销量
     */
    private BigDecimal getPredictedDailySales(Long productId, Integer days) {
        Integer predictedTotal = suggestionMapper.getPredictedSalesTotal(productId, days);
        Integer predictedDays = suggestionMapper.getPredictedSalesDays(productId, days);

        if (predictedTotal == null || predictedTotal == 0) {
            return null;
        }

        if (predictedDays == null || predictedDays == 0) {
            return null;
        }

        return BigDecimal.valueOf(predictedTotal)
                .divide(BigDecimal.valueOf(predictedDays), 2, RoundingMode.HALF_UP);
    }

    /**
     * 创建进货建议记录
     */
    private PurchaseSuggestion createSuggestion(Map<String, Object> product, Integer lightStatus) {
        PurchaseSuggestion suggestion = new PurchaseSuggestion();

        suggestion.setProductId(((Number) product.get("productId")).longValue());
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

        // 计算平滑日均销量
        BigDecimal dailySales = calculateSmoothedDailySales(suggestion.getProductId(), restockCycleDays);
        suggestion.setDailySales(dailySales);

        // 目标库存 = 日均销量 × 补货周期 + 安全库存
        int targetStock = dailySales.multiply(BigDecimal.valueOf(restockCycleDays))
                .add(BigDecimal.valueOf(safetyStock))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
        suggestion.setTargetStock(targetStock);

        // 预测销量 = 日均销量 × 补货周期（用于展示）
        int predictedQuantity = dailySales.multiply(BigDecimal.valueOf(restockCycleDays))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
        suggestion.setPredictedQuantity(predictedQuantity);

        // 建议进货量 = 目标库存 - 当前库存
        int suggestedQuantity = Math.max(0, targetStock - currentStock);
        suggestion.setSuggestedQuantity(suggestedQuantity);
        suggestion.setFinalQuantity(suggestedQuantity);

        suggestion.setStatus(0); // 待处理
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