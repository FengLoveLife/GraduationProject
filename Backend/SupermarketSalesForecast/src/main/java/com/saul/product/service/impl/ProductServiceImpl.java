package com.saul.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.saul.product.dto.ProductAddDTO;
import com.saul.product.dto.ProductQueryDTO;
import com.saul.product.dto.ProductUpdateDTO;
import com.saul.product.entity.Product;
import com.saul.product.mapper.ProductMapper;
import com.saul.product.service.IProductService;
import com.saul.product.vo.ProductSalesSummaryVO;
import com.saul.product.vo.ProductVO;
import com.saul.restocking.entity.PurchaseOrderItem;
import com.saul.restocking.mapper.PurchaseOrderItemMapper;
import com.saul.sales.entity.SalesOrderItem;
import com.saul.sales.mapper.SalesOrderItemMapper;
import com.alibaba.excel.EasyExcel;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 商品 Service 实现类
 */
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements IProductService {

    @Autowired
    private SalesOrderItemMapper salesOrderItemMapper;

    @Autowired
    private PurchaseOrderItemMapper purchaseOrderItemMapper;

    @Override
    public Page<ProductVO> queryPage(ProductQueryDTO queryDTO) {
        // 1. 处理分页参数（防空指针）
        int current = queryDTO.getPage() == null ? 1 : queryDTO.getPage();
        int size = queryDTO.getPageSize() == null ? 5 : queryDTO.getPageSize();
        Page<Product> page = new Page<>(current, size);

        // 2. 构建查询条件
        LambdaQueryWrapper<Product> wrapper = buildQueryWrapper(queryDTO);

        // 3. 执行分页查询
        this.page(page, wrapper);

        // 4. 封装结果 Page<Product> -> Page<ProductVO>
        Page<ProductVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<ProductVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public void exportProduct(ProductQueryDTO queryDTO, String fields, HttpServletResponse response) {
        // 1. 构建查询条件并查询所有数据
        LambdaQueryWrapper<Product> wrapper = buildQueryWrapper(queryDTO);
        List<Product> list = this.list(wrapper);

        // 2. 定义字段映射字典
        Map<String, String> fieldMap = new LinkedHashMap<>();
        fieldMap.put("productCode", "商品编码");
        fieldMap.put("name", "商品名称");
        fieldMap.put("categoryId", "所属分类ID");
        fieldMap.put("specification", "规格");
        fieldMap.put("unit", "单位");
        fieldMap.put("purchasePrice", "进货价");
        fieldMap.put("salePrice", "零售价");
        fieldMap.put("stock", "当前库存");
        fieldMap.put("safetyStock", "安全库存");
        fieldMap.put("status", "状态(1上架/0下架)");

        // 3. 解析前端传入的字段，并强制 productCode 在第一列
        List<String> fieldList = new ArrayList<>();
        fieldList.add("productCode"); // 强制第一列
        if (StringUtils.hasText(fields)) {
            String[] split = fields.split(",");
            for (String f : split) {
                String trimmed = f.trim();
                if (fieldMap.containsKey(trimmed) && !"productCode".equals(trimmed)) {
                    fieldList.add(trimmed);
                }
            }
        } else {
            // 如果没传字段，导出所有
            fieldList.addAll(fieldMap.keySet());
            // 去重（因为 productCode 已经加过了）
            fieldList = new ArrayList<>(new LinkedHashSet<>(fieldList));
        }

        // 4. 构建 EasyExcel 动态表头
        List<List<String>> head = new ArrayList<>();
        for (String field : fieldList) {
            List<String> h = new ArrayList<>();
            h.add(fieldMap.get(field));
            head.add(h);
        }
        // 新增：在表头末尾添加“导出时间”列
        List<String> timeHead = new ArrayList<>();
        timeHead.add("导出时间");
        head.add(timeHead);

        // 5. 构建 EasyExcel 动态数据
        List<List<Object>> dataList = new ArrayList<>();
        for (Product product : list) {
            List<Object> row = new ArrayList<>();
            for (String field : fieldList) {
                row.add(getFieldValue(product, field));
            }
            // 每一行数据的末尾留空，不再重复填充时间
            row.add("");
            dataList.add(row);
        }

        // 6. 特殊处理：仅在第一行数据末尾填充导出时间（原子操作，全局一致）
        if (!dataList.isEmpty()) {
            String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            dataList.get(0).set(dataList.get(0).size() - 1, nowStr);
        }

        // 7. 执行写出
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("商品列表_" + System.currentTimeMillis(), StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            EasyExcel.write(response.getOutputStream())
                    .head(head)
                    .sheet("商品列表")
                    .doWrite(dataList);
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败", e);
        }
    }

    /**
     * 根据字段名获取实体类对应的值
     */
    private Object getFieldValue(Product p, String field) {
        return switch (field) {
            case "productCode" -> p.getProductCode();
            case "name" -> p.getName();
            case "categoryId" -> p.getCategoryId();
            case "specification" -> p.getSpecification();
            case "unit" -> p.getUnit();
            case "purchasePrice" -> p.getPurchasePrice();
            case "salePrice" -> p.getSalePrice();
            case "stock" -> p.getStock();
            case "safetyStock" -> p.getSafetyStock();
            case "status" -> p.getStatus();
            default -> null;
        };
    }

    /**
     * 抽取通用的查询条件构建逻辑
     */
    private LambdaQueryWrapper<Product> buildQueryWrapper(ProductQueryDTO queryDTO) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO == null) return wrapper;

        // 动态拼接分类ID
        wrapper.eq(queryDTO.getCategoryId() != null, Product::getCategoryId, queryDTO.getCategoryId());

        // 动态拼接状态
        wrapper.eq(queryDTO.getStatus() != null, Product::getStatus, queryDTO.getStatus());

        // 核心：关键字模糊搜索
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            String kw = queryDTO.getKeyword().trim();
            wrapper.and(q -> q.like(Product::getName, kw)
                    .or()
                    .like(Product::getProductCode, kw));
        }

        // 默认排序
        wrapper.orderByDesc(Product::getCreateTime).orderByDesc(Product::getId);
        return wrapper;
    }

    @Override
    public ProductVO getDetailById(Long id) {
        Product product = this.getById(id);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        return toVO(product);
    }

    @Override
    public void addProduct(ProductAddDTO addDTO) {
        // 1. 防重校验：商品编码全局唯一
        Long count = this.lambdaQuery()
                .eq(Product::getProductCode, addDTO.getProductCode())
                .count();
        if (count != null && count > 0) {
            throw new RuntimeException("商品编码已存在，请重新输入");
        }

        // 2. DTO -> Entity
        Product product = new Product();
        BeanUtils.copyProperties(addDTO, product);

        // 3. 设置默认状态（如果前端没传）
        if (product.getStatus() == null) {
            product.setStatus(1); // 默认上架
        }

        // 4. 保存入库
        this.save(product);
    }

    @Override
    public void updateProduct(ProductUpdateDTO updateDTO) {
        // 1. 存在性校验
        Product oldProduct = this.getById(updateDTO.getId());
        if (oldProduct == null) {
            throw new RuntimeException("商品不存在");
        }

        // 2. 动态防重校验：如果修改了编码，需检查是否被其他商品占用
        if (StringUtils.hasText(updateDTO.getProductCode())
                && !updateDTO.getProductCode().equals(oldProduct.getProductCode())) {
            Long count = this.lambdaQuery()
                    .eq(Product::getProductCode, updateDTO.getProductCode())
                    .ne(Product::getId, updateDTO.getId())
                    .count();
            if (count != null && count > 0) {
                throw new RuntimeException("商品编码已存在，请重新输入");
            }
        }

        // 3. DTO -> Entity
        Product product = new Product();
        BeanUtils.copyProperties(updateDTO, product);

        // 4. 按需更新（MyBatis-Plus updateById 默认忽略 null 字段）
        this.updateById(product);
    }

    @Override
    public void deleteProduct(Long id) {
        // 关联校验 1：是否有销售记录
        Long salesCount = salesOrderItemMapper.selectCount(
                new LambdaQueryWrapper<SalesOrderItem>().eq(SalesOrderItem::getProductId, id)
        );
        if (salesCount != null && salesCount > 0) {
            throw new RuntimeException("该商品已有销售记录，严禁删除！建议将其状态修改为下架。");
        }

        // 关联校验 2：是否有进货单明细
        Long purchaseCount = purchaseOrderItemMapper.selectCount(
                new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getProductId, id)
        );
        if (purchaseCount != null && purchaseCount > 0) {
            throw new RuntimeException("该商品存在进货历史，无法删除！");
        }

        // 通过校验后执行物理删除
        boolean ok = this.removeById(id);
        if (!ok) {
            throw new RuntimeException("删除商品失败");
        }
    }

    /**
     * 实体类转 VO 辅助方法
     */
    private ProductVO toVO(Product p) {
        if (p == null) return null;
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(p, vo);
        return vo;
    }

    @Override
    public ProductSalesSummaryVO getSalesSummary(Long productId) {
        // 1. 查询商品基础销售汇总
        Map<String, Object> summary = salesOrderItemMapper.getProductSalesSummary(productId);

        ProductSalesSummaryVO vo = new ProductSalesSummaryVO();

        // 2. 填充基础汇总数据
        Integer totalQuantity = summary.get("totalQuantity") != null
                ? ((Number) summary.get("totalQuantity")).intValue() : 0;
        BigDecimal totalAmount = summary.get("totalAmount") != null
                ? new BigDecimal(summary.get("totalAmount").toString()) : BigDecimal.ZERO;
        BigDecimal totalProfit = summary.get("totalProfit") != null
                ? new BigDecimal(summary.get("totalProfit").toString()) : BigDecimal.ZERO;
        Integer saleDays = summary.get("saleDays") != null
                ? ((Number) summary.get("saleDays")).intValue() : 0;
        BigDecimal avgUnitPrice = summary.get("avgUnitPrice") != null
                ? new BigDecimal(summary.get("avgUnitPrice").toString()) : BigDecimal.ZERO;

        vo.setTotalQuantity(totalQuantity);
        vo.setTotalAmount(totalAmount);
        vo.setTotalProfit(totalProfit);
        vo.setAvgUnitPrice(avgUnitPrice.setScale(2, RoundingMode.HALF_UP));
        vo.setSaleDays(saleDays);

        // 3. 计算毛利率
        if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            vo.setProfitRate(totalProfit.divide(totalAmount, 4, RoundingMode.HALF_UP));
        } else {
            vo.setProfitRate(BigDecimal.ZERO);
        }

        // 4. 时间维度
        if (summary.get("firstSaleDate") != null) {
            vo.setFirstSaleDate(((java.sql.Date) summary.get("firstSaleDate")).toLocalDate());
        }
        if (summary.get("lastSaleDate") != null) {
            vo.setLastSaleDate(((java.sql.Date) summary.get("lastSaleDate")).toLocalDate());
        }

        // 5. 日均数据
        if (saleDays > 0) {
            vo.setDailyAvgQuantity(new BigDecimal(totalQuantity)
                    .divide(new BigDecimal(saleDays), 1, RoundingMode.HALF_UP));
            vo.setDailyAvgAmount(totalAmount.divide(new BigDecimal(saleDays), 2, RoundingMode.HALF_UP));
        } else {
            vo.setDailyAvgQuantity(BigDecimal.ZERO);
            vo.setDailyAvgAmount(BigDecimal.ZERO);
        }

        // 6. 近30天趋势
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(29);
        List<Map<String, Object>> recentTrend = salesOrderItemMapper.getProductRecentTrend(productId, startDate.toString());

        // 构建近30天完整日期列表（填补无销售日期）
        List<String> recentDates = new ArrayList<>();
        List<Integer> recentQuantities = new ArrayList<>();
        List<BigDecimal> recentAmounts = new ArrayList<>();

        Map<String, Map<String, Object>> trendMap = recentTrend.stream()
                .collect(Collectors.toMap(
                        m -> m.get("saleDate").toString(),
                        m -> m
                ));

        DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("MM-dd");
        int recent30TotalQty = 0;
        BigDecimal recent30TotalAmount = BigDecimal.ZERO;

        for (int i = 0; i < 30; i++) {
            LocalDate date = startDate.plusDays(i);
            String dateStr = date.toString();
            recentDates.add(date.format(displayFormatter));

            if (trendMap.containsKey(dateStr)) {
                Map<String, Object> dayData = trendMap.get(dateStr);
                int qty = ((Number) dayData.get("quantity")).intValue();
                BigDecimal amt = new BigDecimal(dayData.get("amount").toString());
                recentQuantities.add(qty);
                recentAmounts.add(amt);
                recent30TotalQty += qty;
                recent30TotalAmount = recent30TotalAmount.add(amt);
            } else {
                recentQuantities.add(0);
                recentAmounts.add(BigDecimal.ZERO);
            }
        }

        vo.setRecentDates(recentDates);
        vo.setRecentQuantities(recentQuantities);
        vo.setRecentAmounts(recentAmounts);
        vo.setRecent30Quantity(recent30TotalQty);
        vo.setRecent30Amount(recent30TotalAmount);

        // 7. 近30天 vs 历史日均对比
        if (saleDays > 0 && totalQuantity > 0) {
            BigDecimal historyDailyAvg = new BigDecimal(totalQuantity)
                    .divide(new BigDecimal(saleDays), 2, RoundingMode.HALF_UP);
            BigDecimal recentDailyAvg = new BigDecimal(recent30TotalQty)
                    .divide(new BigDecimal(30), 2, RoundingMode.HALF_UP);

            if (historyDailyAvg.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal growth = recentDailyAvg.subtract(historyDailyAvg)
                        .divide(historyDailyAvg, 4, RoundingMode.HALF_UP);
                vo.setRecentVsHistoryRate(growth);
            } else {
                vo.setRecentVsHistoryRate(BigDecimal.ZERO);
            }
        } else {
            vo.setRecentVsHistoryRate(BigDecimal.ZERO);
        }

        return vo;
    }
}
