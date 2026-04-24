package com.saul.sales.service;

import com.saul.sales.dto.PosOrderDTO;

import java.util.List;
import java.util.Map;

/**
 * POS 订单接收 Service
 * <p>
 * 负责接收 POS/收银系统推送的销售数据，复用与 Excel 导入等价的落库逻辑：
 * 写 sales_order 主表 + sales_order_item 明细 + 扣减库存 + 记 inventory_log 流水。
 */
public interface IPosOrderService {

    /**
     * 接收单笔 POS 订单推送
     *
     * @param dto POS 推送数据
     * @return 处理结果（orderId / itemCount / status）
     */
    Map<String, Object> receivePosOrder(PosOrderDTO dto);

    /**
     * 批量接收 POS 订单（断网补传场景）
     *
     * @param orders 订单列表
     * @return 处理结果（success / failed / duplicate 三个计数）
     */
    Map<String, Object> receivePosOrderBatch(List<PosOrderDTO> orders);

    /**
     * 查询最近 N 条 POS 来源的销售订单
     */
    List<Map<String, Object>> recentPosOrders(int limit);

    /**
     * 聚合统计所有接入过的 POS 终端（按 operator 分组）
     */
    List<Map<String, Object>> listPosTerminals();

    /**
     * POS 推送统计（今日推送数 / 本月推送数 / 累计推送数）
     */
    Map<String, Object> posStatistics();
}
