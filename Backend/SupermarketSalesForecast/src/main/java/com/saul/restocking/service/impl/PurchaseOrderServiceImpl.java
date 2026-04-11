package com.saul.restocking.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.saul.inventory.entity.InventoryLog;
import com.saul.inventory.mapper.InventoryLogMapper;
import com.saul.product.entity.Product;
import com.saul.product.mapper.ProductMapper;
import com.saul.restocking.dto.CreateOrderDTO;
import com.saul.restocking.entity.PurchaseOrder;
import com.saul.restocking.entity.PurchaseOrderItem;
import com.saul.restocking.entity.PurchaseSuggestion;
import com.saul.restocking.mapper.PurchaseOrderItemMapper;
import com.saul.restocking.mapper.PurchaseOrderMapper;
import com.saul.restocking.mapper.PurchaseSuggestionMapper;
import com.saul.restocking.service.IPurchaseOrderService;
import com.saul.restocking.vo.PurchaseOrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 进货单服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl extends ServiceImpl<PurchaseOrderMapper, PurchaseOrder>
        implements IPurchaseOrderService {

    private final PurchaseOrderItemMapper orderItemMapper;
    private final PurchaseSuggestionMapper suggestionMapper;
    private final ProductMapper productMapper;
    private final InventoryLogMapper inventoryLogMapper;

    @Override
    @Transactional
    public Map<String, Object> createOrder(CreateOrderDTO dto, String operator) {
        log.info("开始创建进货单，操作人: {}", operator);

        // 1. 获取进货建议列表
        List<Long> suggestionIds = dto.getItems().stream()
                .map(CreateOrderDTO.OrderItemDTO::getSuggestionId)
                .collect(Collectors.toList());

        LambdaQueryWrapper<PurchaseSuggestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(PurchaseSuggestion::getId, suggestionIds);
        List<PurchaseSuggestion> suggestions = suggestionMapper.selectList(wrapper);

        if (suggestions.isEmpty()) {
            throw new RuntimeException("没有有效的进货建议");
        }

        // 2. 生成进货单号
        String orderNo = generateOrderNo();

        // 3. 创建进货单主表
        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNo(orderNo);
        order.setOrderDate(LocalDate.now());
        order.setStatus(0); // 待确认

        if (dto.getExpectedDate() != null && !dto.getExpectedDate().isEmpty()) {
            order.setExpectedDate(LocalDate.parse(dto.getExpectedDate()));
        }
        order.setRemark(dto.getRemark());
        order.setOperator(operator);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        // 计算总数量和总金额
        int totalQuantity = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        List<PurchaseOrderItem> orderItems = new ArrayList<>();
        Map<Long, Integer> quantityMap = new HashMap<>();

        for (CreateOrderDTO.OrderItemDTO item : dto.getItems()) {
            quantityMap.put(item.getSuggestionId(), item.getQuantity());
        }

        for (PurchaseSuggestion suggestion : suggestions) {
            Integer quantity = quantityMap.getOrDefault(suggestion.getId(), suggestion.getFinalQuantity());

            PurchaseOrderItem orderItem = new PurchaseOrderItem();
            orderItem.setOrderNo(orderNo);
            orderItem.setProductId(suggestion.getProductId());
            orderItem.setProductCode(suggestion.getProductCode());
            orderItem.setProductName(suggestion.getProductName());
            orderItem.setCategoryName(suggestion.getCategoryName());
            orderItem.setPurchasePrice(suggestion.getPurchasePrice() != null ? suggestion.getPurchasePrice() : BigDecimal.ZERO);
            orderItem.setQuantity(quantity);
            orderItem.setSuggestionId(suggestion.getId());

            BigDecimal subtotal = orderItem.getPurchasePrice().multiply(BigDecimal.valueOf(quantity));
            orderItem.setSubtotalAmount(subtotal);
            orderItem.setCreateTime(LocalDateTime.now());

            orderItems.add(orderItem);

            totalQuantity += quantity;
            totalAmount = totalAmount.add(subtotal);
        }

        order.setTotalQuantity(totalQuantity);
        order.setTotalAmount(totalAmount);

        // 4. 保存进货单
        this.save(order);

        // 5. 保存进货明细
        for (PurchaseOrderItem item : orderItems) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        // 6. 更新进货建议状态
        for (PurchaseSuggestion suggestion : suggestions) {
            suggestion.setStatus(1); // 已生成进货单
            suggestion.setUpdateTime(LocalDateTime.now());
            suggestionMapper.updateById(suggestion);
        }

        log.info("进货单创建成功，单号: {}, 商品数: {}, 总金额: {}", orderNo, totalQuantity, totalAmount);

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", order.getId());
        result.put("orderNo", orderNo);
        result.put("totalQuantity", totalQuantity);
        result.put("totalAmount", totalAmount);
        result.put("message", "进货单创建成功");

        return result;
    }

    /**
     * 生成进货单号
     * 格式：PO + 年月日 + 4位序号
     */
    private String generateOrderNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "PO" + dateStr;

        // 查询今天的最大单号
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(PurchaseOrder::getOrderNo, prefix)
                .orderByDesc(PurchaseOrder::getOrderNo)
                .last("LIMIT 1");

        PurchaseOrder lastOrder = this.getOne(wrapper);

        int seq = 1;
        if (lastOrder != null && lastOrder.getOrderNo() != null) {
            String lastNo = lastOrder.getOrderNo();
            if (lastNo.length() >= 14) {
                try {
                    seq = Integer.parseInt(lastNo.substring(10)) + 1;
                } catch (NumberFormatException e) {
                    seq = 1;
                }
            }
        }

        return prefix + String.format("%04d", seq);
    }

    @Override
    public List<PurchaseOrderVO> getOrderList(Integer status, String startDate, String endDate) {
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(PurchaseOrder::getStatus, status);
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(PurchaseOrder::getOrderDate, LocalDate.parse(startDate));
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(PurchaseOrder::getOrderDate, LocalDate.parse(endDate));
        }

        wrapper.orderByDesc(PurchaseOrder::getCreateTime);

        List<PurchaseOrder> orders = this.list(wrapper);
        return orders.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public PurchaseOrderVO getOrderDetail(Long id) {
        PurchaseOrder order = this.getById(id);
        if (order == null) {
            return null;
        }

        PurchaseOrderVO vo = convertToVO(order);

        // 查询明细
        LambdaQueryWrapper<PurchaseOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseOrderItem::getOrderId, id);
        List<PurchaseOrderItem> items = orderItemMapper.selectList(wrapper);

        List<PurchaseOrderVO.OrderItemVO> itemVOs = items.stream().map(item -> {
            PurchaseOrderVO.OrderItemVO itemVO = new PurchaseOrderVO.OrderItemVO();
            itemVO.setId(item.getId());
            itemVO.setProductId(item.getProductId());
            itemVO.setProductCode(item.getProductCode());
            itemVO.setProductName(item.getProductName());
            itemVO.setCategoryName(item.getCategoryName());
            itemVO.setPurchasePrice(item.getPurchasePrice());
            itemVO.setQuantity(item.getQuantity());
            itemVO.setSubtotalAmount(item.getSubtotalAmount());
            return itemVO;
        }).collect(Collectors.toList());

        vo.setItems(itemVOs);
        return vo;
    }

    @Override
    public boolean placeOrder(Long id) {
        PurchaseOrder order = this.getById(id);
        if (order == null || order.getStatus() != 0) {
            return false;
        }
        order.setStatus(1); // 已下单（货在途中）
        order.setUpdateTime(LocalDateTime.now());
        this.updateById(order);
        log.info("进货单已下单，单号: {}", order.getOrderNo());
        return true;
    }

    @Override
    @Transactional
    public boolean confirmArrival(Long id) {
        PurchaseOrder order = this.getById(id);
        if (order == null || order.getStatus() != 1) {
            return false;
        }

        // 查询明细
        LambdaQueryWrapper<PurchaseOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseOrderItem::getOrderId, id);
        List<PurchaseOrderItem> items = orderItemMapper.selectList(wrapper);

        // 更新库存
        for (int i = 0; i < items.size(); i++) {
            PurchaseOrderItem item = items.get(i);
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                int beforeStock = product.getStock();
                int afterStock = beforeStock + item.getQuantity();

                product.setStock(afterStock);
                productMapper.updateById(product);

                // 记录库存变动流水（用订单号+序号保证唯一性）
                InventoryLog inventoryLog = new InventoryLog();
                inventoryLog.setLogNo("IL" + order.getOrderNo() + String.format("%03d", i + 1));
                inventoryLog.setProductId(item.getProductId());
                inventoryLog.setType(1); // 进货入库
                inventoryLog.setChangeAmount(item.getQuantity());
                inventoryLog.setBeforeStock(beforeStock);
                inventoryLog.setAfterStock(afterStock);
                inventoryLog.setReferenceNo(order.getOrderNo());
                inventoryLog.setOperator(order.getOperator());
                inventoryLog.setRemark("进货入库");
                inventoryLog.setCreateTime(LocalDateTime.now());
                inventoryLog.setUpdateTime(LocalDateTime.now());
                inventoryLogMapper.insert(inventoryLog);
            }
        }

        // 更新进货单状态
        order.setStatus(2); // 已完成
        order.setActualArrivalDate(LocalDate.now());
        order.setUpdateTime(LocalDateTime.now());
        this.updateById(order);

        log.info("进货单入库确认成功，单号: {}", order.getOrderNo());
        return true;
    }

    @Override
    @Transactional
    public boolean cancelOrder(Long id) {
        PurchaseOrder order = this.getById(id);
        // 待确认(0) 和 已下单(1) 均可取消
        if (order == null || (order.getStatus() != 0 && order.getStatus() != 1)) {
            return false;
        }

        order.setStatus(3); // 已取消
        order.setUpdateTime(LocalDateTime.now());
        this.updateById(order);

        // 恢复进货建议状态
        LambdaQueryWrapper<PurchaseOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseOrderItem::getOrderId, id);
        List<PurchaseOrderItem> items = orderItemMapper.selectList(wrapper);

        for (PurchaseOrderItem item : items) {
            if (item.getSuggestionId() != null) {
                PurchaseSuggestion suggestion = suggestionMapper.selectById(item.getSuggestionId());
                if (suggestion != null) {
                    suggestion.setStatus(0); // 恢复待处理
                    suggestion.setUpdateTime(LocalDateTime.now());
                    suggestionMapper.updateById(suggestion);
                }
            }
        }

        log.info("进货单已取消，单号: {}", order.getOrderNo());
        return true;
    }

    private PurchaseOrderVO convertToVO(PurchaseOrder order) {
        PurchaseOrderVO vo = new PurchaseOrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalQuantity(order.getTotalQuantity());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setOrderDate(order.getOrderDate());
        vo.setExpectedDate(order.getExpectedDate());
        vo.setActualArrivalDate(order.getActualArrivalDate());
        vo.setStatus(order.getStatus());
        vo.setStatusText(getStatusText(order.getStatus()));
        vo.setRemark(order.getRemark());
        vo.setOperator(order.getOperator());
        vo.setCreateTime(order.getCreateTime());
        return vo;
    }

    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待确认";
            case 1: return "已下单";
            case 2: return "已完成";
            case 3: return "已取消";
            default: return "未知";
        }
    }
}