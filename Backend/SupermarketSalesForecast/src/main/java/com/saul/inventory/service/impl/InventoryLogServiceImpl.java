package com.saul.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.saul.inventory.dto.InventoryLogQueryDTO;
import com.saul.inventory.entity.InventoryLog;
import com.saul.product.entity.Product;
import com.saul.inventory.mapper.InventoryLogMapper;
import com.saul.product.mapper.ProductMapper;
import com.saul.inventory.service.IInventoryLogService;
import com.saul.inventory.vo.InventoryLogVO;
import com.saul.inventory.dto.InventoryAdjustDTO;
import com.alibaba.excel.EasyExcel;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 库存流水 Service 实现类
 */
@Service
@RequiredArgsConstructor
public class InventoryLogServiceImpl extends ServiceImpl<InventoryLogMapper, InventoryLog> implements IInventoryLogService {

    private final ProductMapper productMapper;

    @Override
    public void exportLog(InventoryLogQueryDTO queryDTO, HttpServletResponse response) {
        // 1. 处理 Keyword 关联查询 (复用 queryPage 逻辑)
        Set<Long> productIds = null;
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            String kw = queryDTO.getKeyword().trim();
            List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
                    .and(q -> q.like(Product::getName, kw).or().like(Product::getProductCode, kw)));
            if (products.isEmpty()) {
                // 如果搜不到商品，直接返回空 Excel
                writeEmptyExcel(response);
                return;
            }
            productIds = products.stream().map(Product::getId).collect(Collectors.toSet());
        }

        // 2. 构建查询条件并查询所有数据 (不分页)
        LambdaQueryWrapper<InventoryLog> wrapper = new LambdaQueryWrapper<>();
        if (productIds != null) wrapper.in(InventoryLog::getProductId, productIds);
        wrapper.eq(queryDTO.getType() != null, InventoryLog::getType, queryDTO.getType());
        if (StringUtils.hasText(queryDTO.getStartDate())) {
            wrapper.ge(InventoryLog::getCreateTime, queryDTO.getStartDate() + " 00:00:00");
        }
        if (StringUtils.hasText(queryDTO.getEndDate())) {
            wrapper.le(InventoryLog::getCreateTime, queryDTO.getEndDate() + " 23:59:59");
        }
        wrapper.orderByDesc(InventoryLog::getCreateTime);

        List<InventoryLog> list = this.list(wrapper);

        // 3. 内存关联商品信息
        Set<Long> targetProductIds = list.stream().map(InventoryLog::getProductId).collect(Collectors.toSet());
        Map<Long, Product> productMap = new HashMap<>();
        if (!targetProductIds.isEmpty()) {
            List<Product> productList = productMapper.selectBatchIds(targetProductIds);
            productMap = productList.stream().collect(Collectors.toMap(Product::getId, p -> p));
        }

        // 4. 构建 EasyExcel 表头
        String[] titles = {"流水单号", "变动时间", "商品编码", "商品名称", "变动类型", "变动数量", "变动前库存", "变动后库存", "操作人", "备注", "导出时间"};
        List<List<String>> head = new ArrayList<>();
        for (String title : titles) {
            List<String> h = new ArrayList<>();
            h.add(title);
            head.add(h);
        }

        // 5. 构建数据行
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<List<Object>> dataList = new ArrayList<>();
        for (InventoryLog log : list) {
            List<Object> row = new ArrayList<>();
            row.add(log.getLogNo());
            row.add(log.getCreateTime() != null ? log.getCreateTime().format(dtf) : "");
            
            Product p = productMap.get(log.getProductId());
            row.add(p != null ? p.getProductCode() : "");
            row.add(p != null ? p.getName() : "未知商品");
            
            row.add(getTypeText(log.getType()));
            row.add(log.getChangeAmount());
            row.add(log.getBeforeStock());
            row.add(log.getAfterStock());
            row.add(log.getOperator());
            row.add(log.getRemark());
            row.add(""); // 导出时间占位
            dataList.add(row);
        }

        // 6. 仅在第一行填充导出时间
        if (!dataList.isEmpty()) {
            dataList.get(0).set(dataList.get(0).size() - 1, LocalDateTime.now().format(dtf));
        }

        // 7. 执行写出
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("库存变动流水_" + System.currentTimeMillis(), StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            EasyExcel.write(response.getOutputStream()).head(head).sheet("流水账单").doWrite(dataList);
        } catch (IOException e) {
            throw new RuntimeException("导出流水失败", e);
        }
    }

    private String getTypeText(Integer type) {
        if (type == null) return "未知";
        return switch (type) {
            case 1 -> "进货入库";
            case 2 -> "销售出库";
            case 3 -> "损耗盘亏";
            case 4 -> "手工调整";
            default -> "未知";
        };
    }

    private void writeEmptyExcel(HttpServletResponse response) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("库存变动流水_空_" + System.currentTimeMillis(), StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
            EasyExcel.write(response.getOutputStream()).sheet("无数据").doWrite(Collections.emptyList());
        } catch (IOException ignored) {}
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordStockChange(Long productId, Integer changeAmount, Integer type, String referenceNo, String operator, String remark) {
        // 1. 查询商品是否存在
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("操作失败：商品ID[" + productId + "]不存在");
        }

        // 2. 计算变动后库存
        int beforeStock = product.getStock() == null ? 0 : product.getStock();
        int afterStock = beforeStock + changeAmount;

        // 3. 销售出库防负库存校验
        if (type == 2 && afterStock < 0) {
            throw new RuntimeException("商品[" + product.getName() + "]当前库存(" + beforeStock + ")不足以扣减(" + Math.abs(changeAmount) + ")");
        }

        // 4. 更新商品库存
        Product updateProduct = new Product();
        updateProduct.setId(productId);
        updateProduct.setStock(afterStock);
        productMapper.updateById(updateProduct);

        // 5. 记录流水
        InventoryLog log = new InventoryLog();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomStr = UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();
        log.setLogNo("LOG" + timestamp + randomStr);
        
        log.setProductId(productId);
        log.setType(type);
        log.setChangeAmount(changeAmount);
        log.setBeforeStock(beforeStock);
        log.setAfterStock(afterStock);
        log.setReferenceNo(referenceNo);
        log.setOperator(operator);
        log.setRemark(remark);
        log.setCreateTime(LocalDateTime.now());
        log.setUpdateTime(LocalDateTime.now());

        this.save(log);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustInventory(InventoryAdjustDTO dto, String operator) {
        // 1. 参数校验
        if (dto.getType() != 3 && dto.getType() != 4) {
            throw new RuntimeException("非法变动类型，手工调整仅支持损耗盘亏或手工调整");
        }
        if (dto.getChangeAmount() == 0) {
            throw new RuntimeException("变动数量不能为0");
        }
        if (dto.getType() == 3 && dto.getChangeAmount() > 0) {
            throw new RuntimeException("损耗盘亏类型的变动数量必须小于0");
        }

        // 2. 查询并校验商品
        Product product = productMapper.selectById(dto.getProductId());
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        // 3. 计算并校验库存
        int beforeStock = product.getStock() == null ? 0 : product.getStock();
        int afterStock = beforeStock + dto.getChangeAmount();
        if (afterStock < 0) {
            throw new RuntimeException("调整后的库存不能为负数");
        }

        // 4. 更新商品表
        Product updateProduct = new Product();
        updateProduct.setId(product.getId());
        updateProduct.setStock(afterStock);
        productMapper.updateById(updateProduct);

        // 5. 插入流水表
        InventoryLog log = new InventoryLog();
        // 生成流水号：LOG + yyyyMMddHHmmss + 4位随机码
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomStr = UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();
        log.setLogNo("LOG" + timestamp + randomStr);
        
        log.setProductId(dto.getProductId());
        log.setType(dto.getType());
        log.setChangeAmount(dto.getChangeAmount());
        log.setBeforeStock(beforeStock);
        log.setAfterStock(afterStock);
        log.setOperator(operator);
        log.setRemark(dto.getRemark());
        log.setCreateTime(LocalDateTime.now());
        log.setUpdateTime(LocalDateTime.now());

        this.save(log);
    }

    @Override
    public Page<InventoryLogVO> queryPage(InventoryLogQueryDTO queryDTO) {
        // 1. 处理 Keyword 关联查询
        Set<Long> productIds = null;
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            String kw = queryDTO.getKeyword().trim();
            // 先去商品表搜出匹配的 ID 集合
            List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
                    .and(q -> q.like(Product::getName, kw).or().like(Product::getProductCode, kw)));
            
            if (products.isEmpty()) {
                // 搜了关键词但没找到商品，直接返回空分页
                return new Page<>(queryDTO.getPage(), queryDTO.getPageSize());
            }
            productIds = products.stream().map(Product::getId).collect(Collectors.toSet());
        }

        // 2. 构建流水表查询条件
        LambdaQueryWrapper<InventoryLog> wrapper = new LambdaQueryWrapper<>();
        
        // 关联商品 ID 过滤
        if (productIds != null) {
            wrapper.in(InventoryLog::getProductId, productIds);
        }
        
        // 变动类型过滤
        wrapper.eq(queryDTO.getType() != null, InventoryLog::getType, queryDTO.getType());
        
        // 时间范围过滤
        if (StringUtils.hasText(queryDTO.getStartDate())) {
            wrapper.ge(InventoryLog::getCreateTime, queryDTO.getStartDate() + " 00:00:00");
        }
        if (StringUtils.hasText(queryDTO.getEndDate())) {
            wrapper.le(InventoryLog::getCreateTime, queryDTO.getEndDate() + " 23:59:59");
        }
        
        // 必须按创建时间降序
        wrapper.orderByDesc(InventoryLog::getCreateTime);

        // 3. 执行分页查询
        Page<InventoryLog> logPage = this.page(new Page<>(queryDTO.getPage(), queryDTO.getPageSize()), wrapper);

        // 4. 数据聚合转换 (Entity -> VO)
        Page<InventoryLogVO> voPage = new Page<>(logPage.getCurrent(), logPage.getSize(), logPage.getTotal());
        if (logPage.getRecords().isEmpty()) {
            return voPage;
        }

        // 提取当前页涉及的所有商品 ID 进行批量查询，避免 N+1 问题
        Set<Long> targetProductIds = logPage.getRecords().stream()
                .map(InventoryLog::getProductId)
                .collect(Collectors.toSet());
        
        List<Product> productList = productMapper.selectBatchIds(targetProductIds);
        Map<Long, Product> productMap = productList.stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (v1, v2) -> v1));

        // 组装 VO 列表
        List<InventoryLogVO> voList = logPage.getRecords().stream().map(log -> {
            InventoryLogVO vo = new InventoryLogVO();
            BeanUtils.copyProperties(log, vo);
            
            // 填充商品扩展信息
            Product p = productMap.get(log.getProductId());
            if (p != null) {
                vo.setProductCode(p.getProductCode());
                vo.setProductName(p.getName());
                vo.setImageUrl(p.getImageUrl());
            }
            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }
}
