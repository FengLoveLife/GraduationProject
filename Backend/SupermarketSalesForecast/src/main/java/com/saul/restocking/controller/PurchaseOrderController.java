package com.saul.restocking.controller;

import com.saul.common.Result;
import com.saul.restocking.dto.CreateOrderDTO;
import com.saul.restocking.service.IPurchaseOrderService;
import com.saul.restocking.vo.PurchaseOrderVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 进货单 Controller
 */
@RestController
@RequestMapping("/api/restocking/order")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final IPurchaseOrderService orderService;

    /**
     * 创建进货单
     */
    @PostMapping("/create")
    public Result<Map<String, Object>> createOrder(@RequestBody CreateOrderDTO dto, HttpServletRequest request) {
        // 从请求中获取操作人（实际项目从Token中解析）
        String operator = request.getHeader("X-User-Name");
        if (operator == null || operator.isEmpty()) {
            operator = "系统";
        }

        Map<String, Object> result = orderService.createOrder(dto, operator);
        return Result.success("进货单创建成功", result);
    }

    /**
     * 查询进货单列表
     */
    @GetMapping("/list")
    public Result<List<PurchaseOrderVO>> getOrderList(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        List<PurchaseOrderVO> list = orderService.getOrderList(status, startDate, endDate);
        return Result.success(list);
    }

    /**
     * 查询进货单详情
     */
    @GetMapping("/detail/{id}")
    public Result<PurchaseOrderVO> getOrderDetail(@PathVariable Long id) {
        PurchaseOrderVO detail = orderService.getOrderDetail(id);
        if (detail == null) {
            return Result.error("进货单不存在");
        }
        return Result.success(detail);
    }

    /**
     * 确认入库
     */
    @PutMapping("/confirm/{id}")
    public Result<Boolean> confirmArrival(@PathVariable Long id) {
        boolean success = orderService.confirmArrival(id);
        if (success) {
            return Result.success("入库确认成功", true);
        }
        return Result.error("入库确认失败");
    }

    /**
     * 取消进货单
     */
    @PutMapping("/cancel/{id}")
    public Result<Boolean> cancelOrder(@PathVariable Long id) {
        boolean success = orderService.cancelOrder(id);
        if (success) {
            return Result.success("进货单已取消", true);
        }
        return Result.error("取消失败");
    }
}