package com.saul.restocking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.saul.restocking.entity.PurchaseSuggestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 进货建议 Mapper
 */
@Mapper
public interface PurchaseSuggestionMapper extends BaseMapper<PurchaseSuggestion> {

    /**
     * 查询过去N天的实际销售总量（按商品）
     * @param productId 商品ID
     * @param days 天数
     * @return 总销量
     */
    @Select("SELECT SUM(quantity) as totalQuantity FROM sales_order_item " +
            "WHERE product_id = #{productId} AND sale_date >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY)")
    BigDecimal getHistoricalSalesTotal(@Param("productId") Long productId, @Param("days") Integer days);

    /**
     * 查询过去N天有销售记录的天数
     * @param productId 商品ID
     * @param days 天数
     * @return 实际销售天数
     */
    @Select("SELECT COUNT(DISTINCT sale_date) as salesDays FROM sales_order_item " +
            "WHERE product_id = #{productId} AND sale_date >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY)")
    Integer getHistoricalSalesDays(@Param("productId") Long productId, @Param("days") Integer days);

    /**
     * 查询未来N天的预测销量总和（按商品）
     * @param productId 商品ID
     * @param days 天数
     * @return 预测总销量
     */
    @Select("SELECT SUM(predicted_quantity) as totalPredicted FROM forecast_result " +
            "WHERE product_id = #{productId} AND forecast_date >= CURDATE() AND forecast_date < DATE_ADD(CURDATE(), INTERVAL #{days} DAY)")
    Integer getPredictedSalesTotal(@Param("productId") Long productId, @Param("days") Integer days);

    /**
     * 查询未来N天有预测记录的天数
     * @param productId 商品ID
     * @param days 天数
     * @return 预测天数
     */
    @Select("SELECT COUNT(DISTINCT forecast_date) as forecastDays FROM forecast_result " +
            "WHERE product_id = #{productId} AND forecast_date >= CURDATE() AND forecast_date < DATE_ADD(CURDATE(), INTERVAL #{days} DAY)")
    Integer getPredictedSalesDays(@Param("productId") Long productId, @Param("days") Integer days);

    /**
     * 查询所有红灯商品（按分类分组）
     * @return 红灯商品列表及其分类ID
     */
    @Select("SELECT p.id as productId, p.product_code as productCode, p.name as productName, " +
            "p.category_id as categoryId, pc.name as categoryName, pc.restock_cycle_days as restockCycleDays, " +
            "p.stock as currentStock, p.safety_stock as safetyStock, p.purchase_price as purchasePrice " +
            "FROM product p " +
            "LEFT JOIN product_category pc ON p.category_id = pc.id " +
            "WHERE p.status = 1 AND p.stock <= p.safety_stock")
    List<Map<String, Object>> getRedLightProducts();

    /**
     * 查询指定分类下的所有商品（用于检查黄灯）
     * @param categoryId 分类ID
     * @return 商品列表
     */
    @Select("SELECT p.id as productId, p.product_code as productCode, p.name as productName, " +
            "p.category_id as categoryId, pc.name as categoryName, pc.restock_cycle_days as restockCycleDays, " +
            "p.stock as currentStock, p.safety_stock as safetyStock, p.purchase_price as purchasePrice " +
            "FROM product p " +
            "LEFT JOIN product_category pc ON p.category_id = pc.id " +
            "WHERE p.status = 1 AND p.category_id = #{categoryId}")
    List<Map<String, Object>> getProductsByCategory(@Param("categoryId") Long categoryId);

    /**
     * 查询所有非红灯的上架商品（用于独立扫描黄灯）
     * @return 库存 > 安全库存的上架商品列表
     */
    @Select("SELECT p.id as productId, p.product_code as productCode, p.name as productName, " +
            "p.category_id as categoryId, pc.name as categoryName, pc.restock_cycle_days as restockCycleDays, " +
            "p.stock as currentStock, p.safety_stock as safetyStock, p.purchase_price as purchasePrice " +
            "FROM product p " +
            "LEFT JOIN product_category pc ON p.category_id = pc.id " +
            "WHERE p.status = 1 AND p.stock > p.safety_stock")
    List<Map<String, Object>> getNonRedLightProducts();

    /**
     * 清空待处理的进货建议（重新生成前）
     */
    @Delete("DELETE FROM purchase_suggestion WHERE status = 0")
    void clearPendingSuggestions();

    /**
     * 批量查询所有商品过去N天的历史日均销量
     * @param days 天数
     * @return productId → {totalQuantity, salesDays}
     */
    @Select("SELECT product_id as productId, " +
            "SUM(quantity) as totalQuantity, " +
            "COUNT(DISTINCT sale_date) as salesDays " +
            "FROM sales_order_item " +
            "WHERE sale_date >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
            "GROUP BY product_id")
    List<Map<String, Object>> batchGetHistoricalSales(@Param("days") Integer days);

    /**
     * 批量查询所有商品未来N天的预测日均销量
     * @param days 天数
     * @return productId → {totalPredicted, forecastDays}
     */
    @Select("SELECT product_id as productId, " +
            "SUM(predicted_quantity) as totalPredicted, " +
            "COUNT(DISTINCT forecast_date) as forecastDays " +
            "FROM forecast_result " +
            "WHERE forecast_date >= CURDATE() " +
            "AND forecast_date < DATE_ADD(CURDATE(), INTERVAL #{days} DAY) " +
            "GROUP BY product_id")
    List<Map<String, Object>> batchGetPredictedSales(@Param("days") Integer days);
}