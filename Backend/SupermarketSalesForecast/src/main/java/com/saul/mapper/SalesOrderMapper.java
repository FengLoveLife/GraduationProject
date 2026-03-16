package com.saul.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.saul.entity.SalesOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 销售订单主表 Mapper
 */
@Mapper
public interface SalesOrderMapper extends BaseMapper<SalesOrder> {

    @Select("SELECT IFNULL(SUM(total_amount), 0) as totalAmount, COUNT(id) as orderCount FROM sales_order WHERE sale_date BETWEEN #{startDate} AND #{endDate}")
    Map<String, Object> getOrderKpi(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("SELECT sale_date as saleDate, SUM(total_amount) as dailyAmount FROM sales_order WHERE sale_date BETWEEN #{startDate} AND #{endDate} GROUP BY sale_date ORDER BY sale_date")
    List<Map<String, Object>> getDailyAmount(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("SELECT payment_type as paymentType, SUM(total_amount) as amount FROM sales_order WHERE sale_date BETWEEN #{startDate} AND #{endDate} GROUP BY payment_type")
    List<Map<String, Object>> getPaymentPie(@Param("startDate") String startDate, @Param("endDate") String endDate);
}
