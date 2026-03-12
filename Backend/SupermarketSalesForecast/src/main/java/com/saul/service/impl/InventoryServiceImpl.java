package com.saul.service.impl;

import com.saul.entity.Product;
import com.saul.entity.ProductCategory;
import com.saul.mapper.ProductCategoryMapper;
import com.saul.mapper.ProductMapper;
import com.saul.service.IInventoryService;
import com.saul.vo.ChartDataVO;
import com.saul.vo.InventoryDashboardVO;
import com.saul.vo.KpiVO;
import com.saul.vo.WarningProductVO;
import com.alibaba.excel.EasyExcel;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 库存监控业务实现类
 * 核心逻辑：基于内存 Stream 流动态计算，不依赖额外表
 */
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements IInventoryService {

    private final ProductMapper productMapper;
    private final ProductCategoryMapper categoryMapper;

    @Override
    public List<WarningProductVO> getWarningList(String type) {
        // 1. 基础校验
        if (!"warning".equals(type) && !"soldOut".equals(type)) {
            return Collections.emptyList();
        }

        // 2. 查询所有已上架且设置了安全库存的商品
        List<Product> products = productMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1)
                .isNotNull(Product::getSafetyStock));

        if (products == null || products.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 根据类型进行二次过滤
        List<Product> filteredList = products.stream()
                .filter(p -> {
                    if ("warning".equals(type)) {
                        // 库存告急：0 < stock < safetyStock
                        return p.getStock() != null && p.getStock() > 0 && p.getStock() < p.getSafetyStock();
                    } else {
                        // 已售罄：stock == 0
                        return p.getStock() != null && p.getStock() == 0;
                    }
                })
                .toList();

        if (filteredList.isEmpty()) {
            return Collections.emptyList();
        }

        // 4. 获取分类名称映射 Map
        List<ProductCategory> categories = categoryMapper.selectList(null);
        Map<Long, String> categoryMap = categories.stream()
                .collect(Collectors.toMap(ProductCategory::getId, ProductCategory::getName, (v1, v2) -> v1));

        // 5. 组装 VO 并排序
        return filteredList.stream()
                .map(p -> {
                    WarningProductVO vo = new WarningProductVO();
                    vo.setId(p.getId());
                    vo.setImageUrl(p.getImageUrl());
                    vo.setProductCode(p.getProductCode());
                    vo.setName(p.getName());
                    vo.setCategoryName(categoryMap.getOrDefault(p.getCategoryId(), "未知分类"));
                    vo.setStock(p.getStock());
                    vo.setSafetyStock(p.getSafetyStock());
                    // 计算缺口：stock - safetyStock (负数代表缺口)
                    vo.setGapAmount((p.getStock() == null ? 0 : p.getStock()) - p.getSafetyStock());
                    return vo;
                })
                // 按缺口严重程度排序（gapAmount 越小，缺货越严重，排在越前面）
                .sorted((v1, v2) -> v1.getGapAmount().compareTo(v2.getGapAmount()))
                .collect(Collectors.toList());
    }

    @Override
    public void exportWarningList(HttpServletResponse response) {
        // 1. 查询所有已上架、设置了安全库存且当前库存小于安全库存的商品
        List<Product> products = productMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1)
                .isNotNull(Product::getSafetyStock)
                .apply("stock < safety_stock")); // 使用 apply 处理字段对比

        if (products == null) products = new ArrayList<>();

        // 2. 获取分类名称映射 Map
        List<ProductCategory> categories = categoryMapper.selectList(null);
        Map<Long, String> categoryMap = categories.stream()
                .collect(Collectors.toMap(ProductCategory::getId, ProductCategory::getName, (v1, v2) -> v1));

        // 3. 构建 EasyExcel 表头
        String[] titles = {"商品编码", "商品名称", "所属分类", "预警状态", "当前库存", "安全库存", "缺口数量", "导出时间"};
        List<List<String>> head = new ArrayList<>();
        for (String title : titles) {
            List<String> h = new ArrayList<>();
            h.add(title);
            head.add(h);
        }

        // 4. 构建 EasyExcel 数据行
        List<List<Object>> dataList = new ArrayList<>();
        for (Product p : products) {
            List<Object> row = new ArrayList<>();
            row.add(p.getProductCode());
            row.add(p.getName());
            row.add(categoryMap.getOrDefault(p.getCategoryId(), "未知分类"));
            row.add(p.getStock() == 0 ? "已售罄" : "库存告急");
            row.add(p.getStock());
            row.add(p.getSafetyStock());
            // 缺口数量：安全库存 - 当前库存 (取正数)
            row.add(p.getSafetyStock() - (p.getStock() == null ? 0 : p.getStock()));
            row.add(""); // 导出时间占位
            dataList.add(row);
        }

        // 5. 仅在第一行填充导出时间
        if (!dataList.isEmpty()) {
            String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            dataList.get(0).set(dataList.get(0).size() - 1, nowStr);
        }

        // 6. 执行写出
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("紧急预警商品清单_" + System.currentTimeMillis(), StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            EasyExcel.write(response.getOutputStream())
                    .head(head)
                    .sheet("预警商品")
                    .doWrite(dataList);
        } catch (IOException e) {
            throw new RuntimeException("导出预警清单失败", e);
        }
    }

    @Override
    public InventoryDashboardVO getDashboardData() {
        // 1. 获取所有“上架”状态的商品数据 (状态 1 表示上架)
        List<Product> allProducts = productMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1));
        if (allProducts == null) allProducts = new ArrayList<>();

        InventoryDashboardVO dashboard = new InventoryDashboardVO();

        // 2. 计算 KPI 指标
        KpiVO kpi = calculateKpi(allProducts);
        dashboard.setKpi(kpi);

        // 3. 计算库存健康度分布 (饼图)
        dashboard.setHealthData(calculateHealthData(kpi));

        // 4. 计算各分类缺货排行 (柱状图)
        dashboard.setCategoryWarningData(calculateCategoryWarningData(allProducts));

        return dashboard;
    }

    /**
     * 使用 Stream API 计算核心 KPI
     */
    private KpiVO calculateKpi(List<Product> products) {
        KpiVO kpi = new KpiVO();
        
        // 总 SKU 数
        kpi.setTotalSku(products.size());

        // 库存告急 SKU 数 (0 < stock < safetyStock)
        long warningCount = products.stream()
                .filter(p -> p.getStock() != null && p.getSafetyStock() != null)
                .filter(p -> p.getStock() > 0 && p.getStock() < p.getSafetyStock())
                .count();
        kpi.setWarningSku((int) warningCount);

        // 已售罄 SKU 数 (stock == 0)
        long soldOutCount = products.stream()
                .filter(p -> p.getStock() != null && p.getStock() == 0)
                .count();
        kpi.setSoldOutSku((int) soldOutCount);

        // 当前库存总货值 (Σ purchasePrice * stock)
        BigDecimal totalValue = products.stream()
                .filter(p -> p.getPurchasePrice() != null && p.getStock() != null)
                .map(p -> p.getPurchasePrice().multiply(new BigDecimal(p.getStock())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        kpi.setTotalValue(totalValue);

        return kpi;
    }

    /**
     * 组装健康度分布数据
     */
    private List<ChartDataVO> calculateHealthData(KpiVO kpi) {
        List<ChartDataVO> list = new ArrayList<>();
        int normalCount = kpi.getTotalSku() - kpi.getWarningSku() - kpi.getSoldOutSku();
        
        list.add(new ChartDataVO("健康正常", (long) Math.max(0, normalCount)));
        list.add(new ChartDataVO("库存告急", (long) kpi.getWarningSku()));
        list.add(new ChartDataVO("已经售罄", (long) kpi.getSoldOutSku()));
        
        return list;
    }

    /**
     * 计算各分类缺货排行 (缺货定义：stock < safetyStock)
     * 优化：按一级分类聚合降维
     */
    private List<ChartDataVO> calculateCategoryWarningData(List<Product> products) {
        // 1. 过滤出所有缺货商品 (含售罄)
        List<Product> warningProducts = products.stream()
                .filter(p -> p.getStock() != null && p.getSafetyStock() != null)
                .filter(p -> p.getStock() < p.getSafetyStock())
                .toList();

        if (warningProducts.isEmpty()) return new ArrayList<>();

        // 2. 获取全量分类数据并转为 Map，用于层级溯源
        List<ProductCategory> categories = categoryMapper.selectList(null);
        Map<Long, ProductCategory> categoryMap = categories.stream()
                .collect(Collectors.toMap(ProductCategory::getId, c -> c, (v1, v2) -> v1));

        // 3. 按“一级分类” ID 分组统计缺货 SKU 数量
        Map<Long, Long> warningMap = warningProducts.stream()
                .collect(Collectors.groupingBy(p -> {
                    Long cid = p.getCategoryId();
                    ProductCategory cat = categoryMap.get(cid);
                    if (cat == null) return -1L; // 分类不存在，归为未知
                    
                    // 如果 parentId 为 0 或 null，说明它本身就是一级分类
                    if (cat.getParentId() == null || cat.getParentId() == 0) {
                        return cat.getId();
                    }
                    // 否则返回其父级 ID (即一级分类 ID)
                    return cat.getParentId();
                }, Collectors.counting()));

        // 4. 组装并按数值降序排序
        return warningMap.entrySet().stream()
                .map(entry -> {
                    Long rootId = entry.getKey();
                    String name = "未知分类";
                    if (rootId != -1L) {
                        ProductCategory rootCat = categoryMap.get(rootId);
                        if (rootCat != null) {
                            name = rootCat.getName();
                        }
                    }
                    return new ChartDataVO(name, entry.getValue());
                })
                .sorted((c1, c2) -> c2.getValue().compareTo(c1.getValue()))
                .collect(Collectors.toList());
    }
}
