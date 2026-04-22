package com.saul.sales.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import com.saul.product.entity.Product;
import com.saul.product.mapper.ProductMapper;
import com.saul.sales.dto.SalesImportExcelDTO;
import com.saul.sales.dto.SalesOrderQueryDTO;
import com.saul.sales.entity.SalesOrder;
import com.saul.sales.entity.SalesOrderItem;
import com.saul.sales.mapper.SalesOrderMapper;
import com.saul.sales.service.ISalesOrderItemService;
import com.saul.sales.service.ISalesOrderService;
import com.saul.inventory.service.IInventoryLogService;
import com.saul.sales.vo.SalesOrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 销售订单主表 Service 实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalesOrderServiceImpl extends ServiceImpl<SalesOrderMapper, SalesOrder>
        implements ISalesOrderService {

    private final ProductMapper productMapper;
    private final ISalesOrderItemService salesOrderItemService;
    private final IInventoryLogService inventoryLogService;

    @Override
    public Page<SalesOrderVO> queryPage(SalesOrderQueryDTO queryDTO) {
        // 1. 处理分页
        Page<SalesOrder> page = new Page<>(queryDTO.getPage(), queryDTO.getPageSize());

        // 2. 构建查询条件
        LambdaQueryWrapper<SalesOrder> wrapper = new LambdaQueryWrapper<>();

        // 订单号模糊搜索
        if (StringUtils.hasText(queryDTO.getOrderNo())) {
            wrapper.like(SalesOrder::getOrderNo, queryDTO.getOrderNo().trim());
        }

        // 日期范围查询 (saleDate)
        if (StringUtils.hasText(queryDTO.getStartDate())) {
            wrapper.ge(SalesOrder::getSaleDate, LocalDate.parse(queryDTO.getStartDate()));
        }
        if (StringUtils.hasText(queryDTO.getEndDate())) {
            wrapper.le(SalesOrder::getSaleDate, LocalDate.parse(queryDTO.getEndDate()));
        }

        // 按销售时间倒序
        wrapper.orderByDesc(SalesOrder::getSaleTime);

        // 3. 执行查询
        this.page(page, wrapper);

        // 4. 转换 VO
        Page<SalesOrderVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<SalesOrderVO> voList = page.getRecords().stream().map(order -> {
            SalesOrderVO vo = new SalesOrderVO();
            BeanUtils.copyProperties(order, vo);
            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importSalesData(MultipartFile file, String operator) {
        try {
            // 1. 同步读取 Excel 数据
            List<SalesImportExcelDTO> cachedDataList = EasyExcel.read(file.getInputStream())
                    .head(SalesImportExcelDTO.class)
                    .sheet()
                    .doReadSync();

            if (cachedDataList == null || cachedDataList.isEmpty()) {
                throw new RuntimeException("导入失败：Excel 文件中没有有效数据");
            }

            // 2. 批量查询商品快照信息 (优化：一次查出所有涉及的商品)
            List<String> productCodes = cachedDataList.stream()
                    .map(SalesImportExcelDTO::getProductCode)
                    .distinct()
                    .collect(Collectors.toList());

            Map<String, Product> productMap = productMapper.selectList(new LambdaQueryWrapper<Product>()
                    .in(Product::getProductCode, productCodes))
                    .stream()
                    .collect(Collectors.toMap(Product::getProductCode, p -> p));

            // 3. 按订单号分组处理
            Map<String, List<SalesImportExcelDTO>> orderGroup = cachedDataList.stream()
                    .collect(Collectors.groupingBy(SalesImportExcelDTO::getOrderNo));

            // 3.1 重复导入防护：批量检查本次 Excel 中的订单号是否已存在
            List<String> incomingOrderNos = new ArrayList<>(orderGroup.keySet());
            List<SalesOrder> existingOrders = this.list(new LambdaQueryWrapper<SalesOrder>()
                    .in(SalesOrder::getOrderNo, incomingOrderNos));
            if (!existingOrders.isEmpty()) {
                String duplicates = existingOrders.stream()
                        .map(SalesOrder::getOrderNo)
                        .collect(Collectors.joining("、"));
                throw new RuntimeException("导入失败：以下订单号已存在，请勿重复导入：" + duplicates);
            }

            // 3.2 库存充足性预检：汇总本次 Excel 中各商品的总需求量，一次性与当前库存比对
            // 同一商品可能出现在多张订单中，因此必须聚合后再检查，而非逐行检查
            Map<String, Integer> totalDemandByCode = cachedDataList.stream()
                    .collect(Collectors.groupingBy(
                            SalesImportExcelDTO::getProductCode,
                            Collectors.summingInt(SalesImportExcelDTO::getQuantity)
                    ));
            List<String> stockShortages = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : totalDemandByCode.entrySet()) {
                Product product = productMap.get(entry.getKey());
                if (product == null) continue; // 商品不存在的错误在后续明细处理时报出
                if (product.getStock() < entry.getValue()) {
                    stockShortages.add(String.format("【%s】当前库存 %d 件，本次合计销售 %d 件",
                            product.getName(), product.getStock(), entry.getValue()));
                }
            }
            if (!stockShortages.isEmpty()) {
                throw new RuntimeException("导入失败：以下商品库存不足，请核实 Excel 数据后重新导入：" +
                        String.join("；", stockShortages));
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (Map.Entry<String, List<SalesImportExcelDTO>> entry : orderGroup.entrySet()) {
                String orderNo = entry.getKey();
                List<SalesImportExcelDTO> items = entry.getValue();
                SalesImportExcelDTO firstItem = items.get(0);

                // 4. 构建并保存销售订单主表
                SalesOrder order = new SalesOrder();
                order.setOrderNo(orderNo);
                order.setPaymentType(firstItem.getPaymentType());
                // 优先使用 Excel 中的收银机编号，没有则使用登录操作员
                order.setOperator(StringUtils.hasText(firstItem.getOperator())
                        ? firstItem.getOperator() : operator);

                LocalDateTime saleTime = LocalDateTime.parse(firstItem.getSaleTime(), dtf);
                order.setSaleTime(saleTime);
                order.setSaleDate(saleTime.toLocalDate());

                // 计算订单总额与总数量
                BigDecimal totalAmount = items.stream()
                        .map(i -> i.getUnitPrice().multiply(new BigDecimal(i.getQuantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                int totalQuantity = items.stream().mapToInt(SalesImportExcelDTO::getQuantity).sum();

                order.setTotalAmount(totalAmount);
                order.setTotalQuantity(totalQuantity);
                this.save(order);

                // 5. 构建并保存订单明细，同时联动库存
                List<SalesOrderItem> orderItems = new ArrayList<>();
                for (SalesImportExcelDTO itemDto : items) {
                    Product product = productMap.get(itemDto.getProductCode());
                    if (product == null) {
                        throw new RuntimeException("导入失败：订单[" + orderNo + "]中的商品编码[" + itemDto.getProductCode() + "]在系统中不存在");
                    }

                    SalesOrderItem orderItem = new SalesOrderItem();
                    orderItem.setOrderId(order.getId());
                    orderItem.setOrderNo(orderNo);
                    orderItem.setProductId(product.getId());
                    orderItem.setProductCode(product.getProductCode());
                    orderItem.setProductName(product.getName());
                    orderItem.setCategoryId(product.getCategoryId());

                    orderItem.setPurchasePrice(product.getPurchasePrice());
                    orderItem.setUnitPrice(itemDto.getUnitPrice());
                    orderItem.setQuantity(itemDto.getQuantity());

                    // 计算小计金额与利润
                    BigDecimal subtotalAmount = itemDto.getUnitPrice().multiply(new BigDecimal(itemDto.getQuantity()));
                    BigDecimal subtotalProfit = itemDto.getUnitPrice().subtract(product.getPurchasePrice())
                            .multiply(new BigDecimal(itemDto.getQuantity()));

                    orderItem.setSubtotalAmount(subtotalAmount);
                    orderItem.setSubtotalProfit(subtotalProfit);
                    orderItem.setIsPromotion(itemDto.getIsPromotion());
                    orderItem.setSaleDate(order.getSaleDate());

                    orderItems.add(orderItem);

                    // 6. 联动库存：扣减库存并记流水 (Type=2 销售出库)
                    inventoryLogService.recordStockChange(
                            product.getId(),
                            -itemDto.getQuantity(),
                            2,
                            orderNo,
                            operator,
                            "销售日结 Excel 导入"
                    );
                }
                salesOrderItemService.saveBatch(orderItems);
            }
        } catch (IOException e) {
            throw new RuntimeException("导入失败：读取文件流异常", e);
        }
    }
}
