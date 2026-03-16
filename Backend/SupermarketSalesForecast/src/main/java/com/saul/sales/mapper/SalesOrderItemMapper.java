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

    @Select("SELECT category_name as categoryName, SUM(subtotal_amount) as amount FROM sales_order_item WHERE sale_date BETWEEN #{startDate} AND #{endDate} GROUP BY category_name ORDER BY amount DESC")
    List<Map<String, Object>> getCategoryPie(@Param("startDate") String startDate, @Param("endDate") String endDate);
}
