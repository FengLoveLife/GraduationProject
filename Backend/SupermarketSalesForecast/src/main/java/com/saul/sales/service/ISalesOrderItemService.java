package com.saul.sales.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.saul.sales.entity.SalesOrderItem;
import com.saul.sales.vo.SalesOrderItemVO;
import java.util.List;

/**
 * 销售订单明细表 Service 接口
 */
public interface ISalesOrderItemService extends IService<SalesOrderItem> {
    /**
     * 根据订单ID获取明细列表
     */
    List<SalesOrderItemVO> getItemsByOrderId(Long orderId);
}
