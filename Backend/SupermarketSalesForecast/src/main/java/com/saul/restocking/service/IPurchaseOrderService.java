package com.saul.restocking.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.saul.restocking.dto.CreateOrderDTO;
import com.saul.restocking.entity.PurchaseOrder;
import com.saul.restocking.vo.PurchaseOrderVO;

import java.util.List;
import java.util.Map;

/**
 * 进货单服务接口
 */
public interface IPurchaseOrderService extends IService<PurchaseOrder> {

    /**
     * 创建进货单
     * @param dto 创建请求
     * @param operator 操作人
     * @return 进货单信息
     */
    Map<String, Object> createOrder(CreateOrderDTO dto, String operator);

    /**
     * 查询进货单列表
     * @param status 状态筛选
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 进货单列表
     */
    List<PurchaseOrderVO> getOrderList(Integer status, String startDate, String endDate);

    /**
     * 查询进货单详情
     * @param id 进货单ID
     * @return 进货单详情
     */
    PurchaseOrderVO getOrderDetail(Long id);

    /**
     * 标记已下单（货在途中）
     * @param id 进货单ID
     * @return 是否成功
     */
    boolean placeOrder(Long id);

    /**
     * 确认入库
     * @param id 进货单ID
     * @return 是否成功
     */
    boolean confirmArrival(Long id);

    /**
     * 取消进货单
     * @param id 进货单ID
     * @return 是否成功
     */
    boolean cancelOrder(Long id);
}