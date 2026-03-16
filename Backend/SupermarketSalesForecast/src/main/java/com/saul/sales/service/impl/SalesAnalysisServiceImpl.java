package com.saul.sales.service.impl;

import com.saul.sales.dto.SalesAnalysisQueryDTO;
import com.saul.sales.mapper.SalesOrderItemMapper;
import com.saul.sales.mapper.SalesOrderMapper;
import com.saul.sales.service.ISalesAnalysisService;
import com.saul.sales.vo.ChartDataVO;
import com.saul.sales.vo.SalesKpiVO;
import com.saul.sales.vo.TopProductVO;
import com.saul.sales.vo.TrendChartVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 销售统计分析 Service 实现类
 */
@Service
@RequiredArgsConstructor
public class SalesAnalysisServiceImpl implements ISalesAnalysisService {

    private final SalesOrderMapper salesOrderMapper;
    private final SalesOrderItemMapper salesOrderItemMapper;

    /**
     * 校验日期范围
     */
    private void validateDateRange(String startDate, String endDate) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }
    }

    @Override
    public SalesKpiVO getKpi(SalesAnalysisQueryDTO queryDTO) {
        // 校验日期范围
        validateDateRange(queryDTO.getStartDate(), queryDTO.getEndDate());

        // 1. 查询订单维度的 KPI (销售额、订单数)
        Map<String, Object> orderMap = salesOrderMapper.getOrderKpi(queryDTO.getStartDate(), queryDTO.getEndDate());
        // 2. 查询商品维度的 KPI (毛利、销量)
        Map<String, Object> itemMap = salesOrderItemMapper.getItemKpi(queryDTO.getStartDate(), queryDTO.getEndDate());

        SalesKpiVO vo = new SalesKpiVO();
        
        // 提取并处理空值 (MySQL SUM 可能返回 null)
        BigDecimal totalAmount = orderMap.get("totalAmount") != null ? new BigDecimal(orderMap.get("totalAmount").toString()) : BigDecimal.ZERO;
        Long orderCountLong = orderMap.get("orderCount") != null ? (Long) orderMap.get("orderCount") : 0L;
        Integer orderCount = orderCountLong.intValue();
        
        BigDecimal totalProfit = itemMap.get("totalProfit") != null ? new BigDecimal(itemMap.get("totalProfit").toString()) : BigDecimal.ZERO;
        BigDecimal totalQtyDecimal = itemMap.get("totalQuantity") != null ? new BigDecimal(itemMap.get("totalQuantity").toString()) : BigDecimal.ZERO;
        Integer totalQuantity = totalQtyDecimal.intValue();

        vo.setTotalAmount(totalAmount);
        vo.setOrderCount(orderCount);
        vo.setTotalProfit(totalProfit);
        vo.setTotalQuantity(totalQuantity);

        // 计算客单价 = 销售额 / 订单数
        if (orderCount > 0) {
            vo.setCustomerPrice(totalAmount.divide(new BigDecimal(orderCount), 2, RoundingMode.HALF_UP));
        } else {
            vo.setCustomerPrice(BigDecimal.ZERO);
        }

        // 计算毛利率 = 毛利润 / 销售额
        if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            vo.setProfitRate(totalProfit.divide(totalAmount, 4, RoundingMode.HALF_UP)); // 保留4位小数，前端转百分比
        } else {
            vo.setProfitRate(BigDecimal.ZERO);
        }

        return vo;
    }

    @Override
    public TrendChartVO getTrend(SalesAnalysisQueryDTO queryDTO) {
        // 校验日期范围
        validateDateRange(queryDTO.getStartDate(), queryDTO.getEndDate());

        // 1. 分别查询每日销售额和每日毛利
        List<Map<String, Object>> amountList = salesOrderMapper.getDailyAmount(queryDTO.getStartDate(), queryDTO.getEndDate());
        List<Map<String, Object>> profitList = salesOrderItemMapper.getDailyProfit(queryDTO.getStartDate(), queryDTO.getEndDate());

        // 2. 转换为 Map 方便查找 (Key: LocalDateString, Value: BigDecimal)
        Map<String, BigDecimal> amountMap = amountList.stream().collect(Collectors.toMap(
                m -> m.get("saleDate").toString(),
                m -> new BigDecimal(m.get("dailyAmount").toString())
        ));
        
        Map<String, BigDecimal> profitMap = profitList.stream().collect(Collectors.toMap(
                m -> m.get("saleDate").toString(),
                m -> new BigDecimal(m.get("dailyProfit").toString())
        ));

        // 3. 收集所有涉及的日期并排序 (合并两个列表的日期)
        Set<String> dateSet = new HashSet<>();
        dateSet.addAll(amountMap.keySet());
        dateSet.addAll(profitMap.keySet());
        
        List<String> sortedDates = new ArrayList<>(dateSet);
        Collections.sort(sortedDates);

        // 4. 组装最终数据
        List<BigDecimal> finalAmounts = new ArrayList<>();
        List<BigDecimal> finalProfits = new ArrayList<>();
        List<String> displayDates = new ArrayList<>();

        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("MM-dd");

        for (String dateStr : sortedDates) {
            // 格式化日期展示 (去掉年份)
            try {
                LocalDate date = LocalDate.parse(dateStr, inputFormatter);
                displayDates.add(date.format(outputFormatter));
            } catch (Exception e) {
                displayDates.add(dateStr); // 兜底
            }

            finalAmounts.add(amountMap.getOrDefault(dateStr, BigDecimal.ZERO));
            finalProfits.add(profitMap.getOrDefault(dateStr, BigDecimal.ZERO));
        }

        TrendChartVO vo = new TrendChartVO();
        vo.setDates(displayDates);
        vo.setAmounts(finalAmounts);
        vo.setProfits(finalProfits);
        
        return vo;
    }

    @Override
    public List<TopProductVO> getTopProducts(SalesAnalysisQueryDTO queryDTO) {
        // 校验日期范围
        validateDateRange(queryDTO.getStartDate(), queryDTO.getEndDate());

        return salesOrderItemMapper.getTop10Products(queryDTO.getStartDate(), queryDTO.getEndDate());
    }

    @Override
    public List<ChartDataVO> getCategoryPie(SalesAnalysisQueryDTO queryDTO) {
        // 校验日期范围
        validateDateRange(queryDTO.getStartDate(), queryDTO.getEndDate());

        List<Map<String, Object>> list = salesOrderItemMapper.getCategoryPie(queryDTO.getStartDate(), queryDTO.getEndDate());
        
        return list.stream().map(m -> {
            String name = m.get("categoryName") != null ? m.get("categoryName").toString() : "未分类";
            if (name.trim().isEmpty()) {
                name = "未分类";
            }
            
            BigDecimal value = m.get("amount") != null ? new BigDecimal(m.get("amount").toString()) : BigDecimal.ZERO;
            return new ChartDataVO(name, value);
        }).collect(Collectors.toList());
    }

    @Override
    public List<ChartDataVO> getPaymentPie(SalesAnalysisQueryDTO queryDTO) {
        // 校验日期范围
        validateDateRange(queryDTO.getStartDate(), queryDTO.getEndDate());

        List<Map<String, Object>> list = salesOrderMapper.getPaymentPie(queryDTO.getStartDate(), queryDTO.getEndDate());
        
        return list.stream().map(m -> {
            Object typeObj = m.get("paymentType");
            int type = -1;
            if (typeObj != null) {
                try {
                    type = Integer.parseInt(typeObj.toString());
                } catch (NumberFormatException ignored) {}
            }
            
            String name = switch (type) {
                case 1 -> "现金支付";
                case 2 -> "微信支付";
                case 3 -> "支付宝支付";
                default -> "未知支付";
            };
            
            BigDecimal value = m.get("amount") != null ? new BigDecimal(m.get("amount").toString()) : BigDecimal.ZERO;
            return new ChartDataVO(name, value);
        }).collect(Collectors.toList());
    }
}
