package com.saul.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.saul.dto.InventoryAdjustDTO;
import com.saul.dto.InventoryLogQueryDTO;
import com.saul.entity.InventoryLog;
import com.saul.vo.InventoryLogVO;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 库存流水 Service 接口
 */
public interface IInventoryLogService extends IService<InventoryLog> {
    /**
     * 分页查询库存流水
     */
    Page<InventoryLogVO> queryPage(InventoryLogQueryDTO queryDTO);

    /**
     * 手工盘点调整库存
     * @param dto 调整参数
     * @param operator 操作人
     */
    void adjustInventory(InventoryAdjustDTO dto, String operator);

    /**
     * 统一库存变动记录接口（供销售、进货等模块联动调用）
     * @param productId 商品ID
     * @param changeAmount 变动数量（正数为加，负数为减）
     * @param type 变动类型: 1进货入库, 2销售出库, 3损耗盘亏, 4手工调整
     * @param referenceNo 关联业务单号
     * @param operator 操作人
     * @param remark 备注
     */
    void recordStockChange(Long productId, Integer changeAmount, Integer type, String referenceNo, String operator, String remark);

    /**
     * 导出库存流水 Excel
     * @param queryDTO 查询条件
     * @param response 响应对象
     */
    void exportLog(InventoryLogQueryDTO queryDTO, HttpServletResponse response);
}
