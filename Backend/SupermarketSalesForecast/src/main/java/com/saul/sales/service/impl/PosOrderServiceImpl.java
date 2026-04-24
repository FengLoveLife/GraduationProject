package com.saul.sales.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.saul.inventory.service.IInventoryLogService;
import com.saul.product.entity.Product;
import com.saul.product.mapper.ProductMapper;
import com.saul.sales.dto.PosOrderDTO;
import com.saul.sales.entity.SalesOrder;
import com.saul.sales.entity.SalesOrderItem;
import com.saul.sales.service.ISalesOrderItemService;
import com.saul.sales.service.ISalesOrderService;
import com.saul.sales.service.IPosOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * POS 订单接收服务实现
 * <p>
 * 核心逻辑与 {@link SalesOrderServiceImpl#importSalesData} 保持完全一致：
 * 幂等 → 库存校验 → 订单入库 → 明细入库 → 扣库存 → 记流水。
 * 唯一区别是数据入口不同：Excel 批量 vs POS 单笔实时。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PosOrderServiceImpl implements IPosOrderService {

    private final ISalesOrderService salesOrderService;
    private final ISalesOrderItemService salesOrderItemService;
    private final ProductMapper productMapper;
    private final IInventoryLogService inventoryLogService;

    private static final DateTimeFormatter SALE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> receivePosOrder(PosOrderDTO dto) {
        // 1. 基础参数校验
        if (dto == null || !StringUtils.hasText(dto.getOrderNo())) {
            throw new RuntimeException("POS 推送失败：订单号不能为空");
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new RuntimeException("POS 推送失败：订单明细不能为空");
        }

        // 2. 幂等检查（订单号已存在则直接返回，不抛异常 —— 允许 POS 端安全重试）
        long existing = salesOrderService.count(
                new LambdaQueryWrapper<SalesOrder>().eq(SalesOrder::getOrderNo, dto.getOrderNo())
        );
        if (existing > 0) {
            log.warn("[POS] 订单号已存在，忽略重复推送: {}", dto.getOrderNo());
            Map<String, Object> dup = new HashMap<>();
            dup.put("status", "DUPLICATE");
            dup.put("orderNo", dto.getOrderNo());
            dup.put("message", "订单已存在，忽略重复推送");
            return dup;
        }

        // 3. 批量加载商品快照（用 productCode -> Product 的 Map 代替逐行查询）
        List<String> productCodes = dto.getItems().stream()
                .map(PosOrderDTO.PosOrderItemDTO::getProductCode)
                .distinct()
                .collect(Collectors.toList());
        Map<String, Product> productMap = productMapper.selectList(
                        new LambdaQueryWrapper<Product>().in(Product::getProductCode, productCodes))
                .stream()
                .collect(Collectors.toMap(Product::getProductCode, p -> p));

        // 4. 库存充足性预检（汇总同一商品在订单内的总数量，一次性校验）
        Map<String, Integer> totalDemand = dto.getItems().stream()
                .collect(Collectors.groupingBy(
                        PosOrderDTO.PosOrderItemDTO::getProductCode,
                        Collectors.summingInt(PosOrderDTO.PosOrderItemDTO::getQuantity)
                ));
        List<String> shortages = new ArrayList<>();
        for (Map.Entry<String, Integer> e : totalDemand.entrySet()) {
            Product p = productMap.get(e.getKey());
            if (p == null) {
                throw new RuntimeException("POS 推送失败：商品编码[" + e.getKey() + "]在系统中不存在");
            }
            if (p.getStock() < e.getValue()) {
                shortages.add(String.format("【%s】库存 %d 件，本单需要 %d 件",
                        p.getName(), p.getStock(), e.getValue()));
            }
        }
        if (!shortages.isEmpty()) {
            throw new RuntimeException("POS 推送失败：库存不足 —— " + String.join("；", shortages));
        }

        // 5. 解析销售时间
        LocalDateTime saleTime;
        try {
            saleTime = LocalDateTime.parse(dto.getSaleTime(), SALE_TIME_FMT);
        } catch (Exception ex) {
            throw new RuntimeException("POS 推送失败：销售时间格式错误，应为 yyyy-MM-dd HH:mm:ss");
        }
        LocalDate saleDate = StringUtils.hasText(dto.getSaleDate())
                ? LocalDate.parse(dto.getSaleDate()) : saleTime.toLocalDate();

        // 6. 构建主订单
        SalesOrder order = new SalesOrder();
        order.setOrderNo(dto.getOrderNo());
        order.setPaymentType(dto.getPaymentType() != null ? dto.getPaymentType() : 2);
        order.setSaleTime(saleTime);
        order.setSaleDate(saleDate);
        // operator 统一存放 POS 机编号，与 Excel 导入的"收银机编号"字段语义一致
        order.setOperator(StringUtils.hasText(dto.getPosId()) ? dto.getPosId() : "POS-UNKNOWN");

        BigDecimal totalAmount = dto.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalQuantity = dto.getItems().stream()
                .mapToInt(PosOrderDTO.PosOrderItemDTO::getQuantity).sum();
        order.setTotalAmount(totalAmount);
        order.setTotalQuantity(totalQuantity);
        salesOrderService.save(order);

        // 7. 构建明细 + 联动库存扣减
        List<SalesOrderItem> orderItems = new ArrayList<>();
        for (PosOrderDTO.PosOrderItemDTO itemDto : dto.getItems()) {
            Product product = productMap.get(itemDto.getProductCode());

            SalesOrderItem item = new SalesOrderItem();
            item.setOrderId(order.getId());
            item.setOrderNo(order.getOrderNo());
            item.setProductId(product.getId());
            item.setProductCode(product.getProductCode());
            item.setProductName(product.getName());
            item.setCategoryId(product.getCategoryId());
            item.setPurchasePrice(product.getPurchasePrice());
            item.setUnitPrice(itemDto.getUnitPrice());
            item.setQuantity(itemDto.getQuantity());

            BigDecimal subtotalAmount = itemDto.getUnitPrice()
                    .multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            BigDecimal subtotalProfit = itemDto.getUnitPrice()
                    .subtract(product.getPurchasePrice())
                    .multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            item.setSubtotalAmount(subtotalAmount);
            item.setSubtotalProfit(subtotalProfit);
            item.setIsPromotion(itemDto.getIsPromotion() != null ? itemDto.getIsPromotion() : 0);
            item.setSaleDate(saleDate);

            orderItems.add(item);

            // 库存扣减 + 流水记录（type=2 销售出库）
            inventoryLogService.recordStockChange(
                    product.getId(),
                    -itemDto.getQuantity(),
                    2,
                    order.getOrderNo(),
                    order.getOperator(),
                    "POS 实时推送"
            );
        }
        salesOrderItemService.saveBatch(orderItems);

        log.info("[POS] 推送入库成功: orderNo={}, posId={}, items={}, amount={}",
                order.getOrderNo(), order.getOperator(), orderItems.size(), totalAmount);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "SUCCESS");
        result.put("orderId", order.getId());
        result.put("orderNo", order.getOrderNo());
        result.put("itemCount", orderItems.size());
        result.put("totalAmount", totalAmount);
        return result;
    }

    @Override
    public Map<String, Object> receivePosOrderBatch(List<PosOrderDTO> orders) {
        int success = 0, failed = 0, duplicate = 0;
        List<String> failedOrderNos = new ArrayList<>();

        for (PosOrderDTO order : orders) {
            try {
                Map<String, Object> r = receivePosOrder(order);
                if ("DUPLICATE".equals(r.get("status"))) {
                    duplicate++;
                } else {
                    success++;
                }
            } catch (Exception e) {
                failed++;
                failedOrderNos.add(order.getOrderNo() + "(" + e.getMessage() + ")");
                log.error("[POS] 批量推送单笔失败: {} -> {}", order.getOrderNo(), e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", orders.size());
        result.put("success", success);
        result.put("duplicate", duplicate);
        result.put("failed", failed);
        result.put("failedDetail", failedOrderNos);
        return result;
    }

    @Override
    public List<Map<String, Object>> recentPosOrders(int limit) {
        // 基于 operator LIKE 'POS%' 识别 POS 来源订单
        List<SalesOrder> list = salesOrderService.list(
                new LambdaQueryWrapper<SalesOrder>()
                        .likeRight(SalesOrder::getOperator, "POS")
                        .orderByDesc(SalesOrder::getSaleTime)
                        .last("LIMIT " + Math.max(1, Math.min(limit, 100)))
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (SalesOrder o : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", o.getId());
            m.put("orderNo", o.getOrderNo());
            m.put("posId", o.getOperator());
            m.put("saleTime", o.getSaleTime());
            m.put("totalAmount", o.getTotalAmount());
            m.put("totalQuantity", o.getTotalQuantity());
            m.put("paymentType", o.getPaymentType());
            result.add(m);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> listPosTerminals() {
        // 按 operator 分组，展示每台 POS 机的活跃情况
        List<SalesOrder> all = salesOrderService.list(
                new LambdaQueryWrapper<SalesOrder>()
                        .likeRight(SalesOrder::getOperator, "POS")
                        .orderByDesc(SalesOrder::getSaleTime)
        );

        Map<String, List<SalesOrder>> grouped = all.stream()
                .collect(Collectors.groupingBy(SalesOrder::getOperator));

        List<Map<String, Object>> terminals = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (Map.Entry<String, List<SalesOrder>> e : grouped.entrySet()) {
            List<SalesOrder> orders = e.getValue();
            long todayCount = orders.stream()
                    .filter(o -> today.equals(o.getSaleDate()))
                    .count();
            LocalDateTime lastActive = orders.stream()
                    .map(SalesOrder::getSaleTime)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);

            Map<String, Object> t = new HashMap<>();
            t.put("posId", e.getKey());
            t.put("totalOrders", orders.size());
            t.put("todayOrders", todayCount);
            t.put("lastActiveTime", lastActive);
            // 最近 30 分钟内活跃视为"在线"
            boolean online = lastActive != null
                    && lastActive.isAfter(LocalDateTime.now().minusMinutes(30));
            t.put("status", online ? "ONLINE" : "IDLE");
            terminals.add(t);
        }
        terminals.sort((a, b) -> {
            LocalDateTime ta = (LocalDateTime) a.get("lastActiveTime");
            LocalDateTime tb = (LocalDateTime) b.get("lastActiveTime");
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        });
        return terminals;
    }

    @Override
    public Map<String, Object> posStatistics() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        long todayCount = salesOrderService.count(
                new LambdaQueryWrapper<SalesOrder>()
                        .likeRight(SalesOrder::getOperator, "POS")
                        .eq(SalesOrder::getSaleDate, today)
        );
        long monthCount = salesOrderService.count(
                new LambdaQueryWrapper<SalesOrder>()
                        .likeRight(SalesOrder::getOperator, "POS")
                        .ge(SalesOrder::getSaleDate, monthStart)
        );
        long totalCount = salesOrderService.count(
                new LambdaQueryWrapper<SalesOrder>()
                        .likeRight(SalesOrder::getOperator, "POS")
        );
        long terminalCount = salesOrderService.list(
                        new LambdaQueryWrapper<SalesOrder>().likeRight(SalesOrder::getOperator, "POS"))
                .stream().map(SalesOrder::getOperator).distinct().count();

        Map<String, Object> r = new HashMap<>();
        r.put("todayCount", todayCount);
        r.put("monthCount", monthCount);
        r.put("totalCount", totalCount);
        r.put("terminalCount", terminalCount);
        return r;
    }
}
