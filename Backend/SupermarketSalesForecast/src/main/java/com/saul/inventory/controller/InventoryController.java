package com.saul.inventory.controller;

import com.saul.common.Result;
import com.saul.inventory.service.IInventoryService;
import com.saul.inventory.vo.InventoryDashboardVO;
import com.saul.inventory.vo.WarningProductVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 库存监控与预警 Controller
 */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final IInventoryService inventoryService;

    /**
     * 导出库存预警商品 Excel
     */
    @GetMapping("/export-warning")
    public void exportWarning(HttpServletResponse response) {
        inventoryService.exportWarningList(response);
    }

    /**
     * 获取库存看板聚合数据
     * @return 统一返回格式的看板数据
     */
    @GetMapping("/dashboard")
    public Result<InventoryDashboardVO> getDashboard() {
        InventoryDashboardVO data = inventoryService.getDashboardData();
        return Result.success(data);
    }

    /**
     * 获取预警商品明细列表
     * @param type 预警类型：warning-库存告急, soldOut-已售罄
     * @return 预警商品列表
     */
    @GetMapping("/warning-list")
    public Result<List<WarningProductVO>> warningList(@RequestParam("type") String type) {
        List<WarningProductVO> list = inventoryService.getWarningList(type);
        return Result.success(list);
    }
}
