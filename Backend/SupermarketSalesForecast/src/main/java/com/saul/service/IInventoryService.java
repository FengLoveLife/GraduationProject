package com.saul.service;

import com.saul.vo.InventoryDashboardVO;
import com.saul.vo.WarningProductVO;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 库存监控与预警业务接口
 */
public interface IInventoryService {

    /**
     * 获取库存看板聚合数据
     * @return 包含KPI、健康度分布、分类缺货排行的数据对象
     */
    InventoryDashboardVO getDashboardData();

    /**
     * 获取预警商品明细列表
     * @param type 预警类型：warning-库存告急, soldOut-已售罄
     * @return 预警商品列表
     */
    List<WarningProductVO> getWarningList(String type);

    /**
     * 导出库存预警商品 Excel
     * @param response 响应对象
     */
    void exportWarningList(HttpServletResponse response);
}
