package com.saul.sales.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.saul.sales.entity.SalesOrderItem;
import com.saul.sales.vo.TopProductVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 销售订单明细表 Mapper
 */
@Mapper
public interface SalesOrderItemMapper extends BaseMapper<SalesOrderItem> {

    @Select("SELECT IFNULL(SUM(subtotal_profit), 0) as totalProfit, IFNULL(SUM(quantity), 0) as totalQuantity FROM sales_order_item WHERE sale_date BETWEEN #{startDate} AND #{endDate}")
    Map<String, Object> getItemKpi(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("SELECT sale_date as saleDate, SUM(subtotal_profit) as dailyProfit FROM sales_order_item WHERE sale_date BETWEEN #{startDate} AND #{endDate} GROUP BY sale_date ORDER BY sale_date")
    List<Map<String, Object>> getDailyProfit(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("SELECT product_name as productName, SUM(quantity) as quantity, SUM(subtotal_amount) as amount FROM sales_order_item WHERE sale_date BETWEEN #{startDate} AND #{endDate} GROUP BY product_name ORDER BY quantity DESC LIMIT 10")
    List<TopProductVO> getTop10Products(@Param("startDate") String startDate, @Param("endDate") String endDate);

    // 通过 product_id → product → 二级分类 → 一级分类，与其他分类查询保持一致
    @Select("SELECT COALESCE(pc1.name, '未分类') as categoryName, SUM(soi.subtotal_amount) as amount " +
            "FROM sales_order_item soi " +
            "LEFT JOIN product p ON soi.product_id = p.id " +
            "LEFT JOIN product_category pc2 ON p.category_id = pc2.id " +
            "LEFT JOIN product_category pc1 ON pc2.parent_id = pc1.id " +
            "WHERE soi.sale_date BETWEEN #{startDate} AND #{endDate} " +
            "GROUP BY categoryName ORDER BY amount DESC")
    List<Map<String, Object>> getCategoryPie(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 查询指定日期的各商品实际销量汇总
     */
    @Select("SELECT product_id as productId, SUM(quantity) as actualQuantity FROM sales_order_item WHERE sale_date = #{date} GROUP BY product_id")
    List<Map<String, Object>> getActualQuantityByDate(@Param("date") String date);

    /**
     * 查询日期范围内的实际销售数据（按日期 + 商品汇总）
     */
    @Select("<script>" +
            "SELECT DATE(sale_date) as saleDate, product_id as productId, category_id as categoryId, SUM(quantity) as actualQuantity " +
            "FROM sales_order_item " +
            "WHERE sale_date BETWEEN #{startDate} AND #{endDate} " +
            "<if test='productId != null'>AND product_id = #{productId}</if>" +
            "<if test='categoryId != null'>AND category_id = #{categoryId}</if>" +
            "GROUP BY DATE(sale_date), product_id, category_id" +
            "</script>")
    List<Map<String, Object>> getActualSalesByDate(@Param("startDate") String startDate, @Param("endDate") String endDate,
                                                    @Param("productId") Long productId, @Param("categoryId") Long categoryId);

    /**
     * 查询日期范围内的实际销售数据（按日期汇总）
     */
    @Select("SELECT DATE(sale_date) as saleDate, SUM(quantity) as actualQuantity FROM sales_order_item WHERE sale_date BETWEEN #{startDate} AND #{endDate} GROUP BY DATE(sale_date)")
    List<Map<String, Object>> getActualSalesSumByDate(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 查询单日销售汇总（销量和销售额）
     */
    @Select("SELECT IFNULL(SUM(quantity), 0) as totalQuantity, IFNULL(SUM(subtotal_amount), 0) as totalAmount FROM sales_order_item WHERE sale_date = #{date}")
    Map<String, Object> getDailySalesSummary(@Param("date") String date);

    /**
     * 查询指定日期按一级分类汇总的销量
     */
    @Select("SELECT COALESCE(pc1.name, '未分类') as categoryName, SUM(soi.quantity) as totalQuantity " +
            "FROM sales_order_item soi " +
            "LEFT JOIN product p ON soi.product_id = p.id " +
            "LEFT JOIN product_category pc2 ON p.category_id = pc2.id " +
            "LEFT JOIN product_category pc1 ON pc2.parent_id = pc1.id " +
            "WHERE soi.sale_date = #{date} " +
            "GROUP BY categoryName " +
            "ORDER BY totalQuantity DESC")
    List<Map<String, Object>> getCategoryQuantityByDate(@Param("date") String date);

    /**
     * 查询日期范围内按一级分类汇总的销量（用于降级展示）
     */
    @Select("SELECT COALESCE(pc1.name, '未分类') as categoryName, SUM(soi.quantity) as totalQuantity " +
            "FROM sales_order_item soi " +
            "LEFT JOIN product p ON soi.product_id = p.id " +
            "LEFT JOIN product_category pc2 ON p.category_id = pc2.id " +
            "LEFT JOIN product_category pc1 ON pc2.parent_id = pc1.id " +
            "WHERE soi.sale_date BETWEEN #{startDate} AND #{endDate} " +
            "GROUP BY categoryName " +
            "ORDER BY totalQuantity DESC")
    List<Map<String, Object>> getCategoryQuantityByDateRange(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 查询商品销售汇总（总销量、总销售额、总毛利、首次/最近销售日期、销售天数）
     */
    @Select("SELECT " +
            "IFNULL(SUM(quantity), 0) as totalQuantity, " +
            "IFNULL(SUM(subtotal_amount), 0) as totalAmount, " +
            "IFNULL(SUM(subtotal_profit), 0) as totalProfit, " +
            "MIN(sale_date) as firstSaleDate, " +
            "MAX(sale_date) as lastSaleDate, " +
            "COUNT(DISTINCT sale_date) as saleDays, " +
            "IFNULL(AVG(unit_price), 0) as avgUnitPrice " +
            "FROM sales_order_item " +
            "WHERE product_id = #{productId}")
    Map<String, Object> getProductSalesSummary(@Param("productId") Long productId);

    /**
     * 批量查询多个商品在指定日期范围内的日均销量
     * 用于计算历史日均，支撑不同补货周期的进货量公式
     */
    @Select("SELECT product_id as productId, " +
            "ROUND(SUM(quantity) / NULLIF(COUNT(DISTINCT sale_date), 0), 2) as dailyAvg " +
            "FROM sales_order_item " +
            "WHERE sale_date BETWEEN #{startDate} AND #{endDate} " +
            "GROUP BY product_id")
    List<Map<String, Object>> getProductDailyAvgBatch(@Param("startDate") String startDate,
                                                      @Param("endDate") String endDate);

    /**
     * 查询商品近N天的销售趋势（按日期汇总）
     */
    @Select("SELECT DATE(sale_date) as saleDate, " +
            "SUM(quantity) as quantity, " +
            "SUM(subtotal_amount) as amount " +
            "FROM sales_order_item " +
            "WHERE product_id = #{productId} " +
            "AND sale_date >= #{startDate} " +
            "GROUP BY DATE(sale_date) " +
            "ORDER BY DATE(sale_date)")
    List<Map<String, Object>> getProductRecentTrend(@Param("productId") Long productId, @Param("startDate") String startDate);
}
