package com.saul.sales.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.saul.common.Result;
import com.saul.sales.dto.SalesOrderQueryDTO;
import com.saul.sales.service.ISalesOrderItemService;
import com.saul.sales.service.ISalesOrderService;
import com.saul.sales.vo.SalesOrderItemVO;
import com.saul.sales.vo.SalesOrderVO;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 销售数据中心 Controller
 */
@RestController
@RequestMapping("/api/sales-order")
@RequiredArgsConstructor
public class SalesOrderController {

    private final ISalesOrderService salesOrderService;
    private final ISalesOrderItemService salesOrderItemService;

    /**
     * 分页查询销售订单
     */
    @GetMapping("/page")
    public Result<Page<SalesOrderVO>> page(SalesOrderQueryDTO queryDTO) {
        return Result.success(salesOrderService.queryPage(queryDTO));
    }

    /**
     * 获取订单明细
     */
    @GetMapping("/{id}/items")
    public Result<List<SalesOrderItemVO>> getItems(@PathVariable("id") Long id) {
        return Result.success(salesOrderItemService.getItemsByOrderId(id));
    }

    /**
     * 导入销售日结 Excel 数据
     */
    @PostMapping("/import")
    public Result<String> importSales(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        // 1. 从请求属性中获取操作人信息
        Claims claims = (Claims) request.getAttribute("jwtClaims");
        String operator = "系统管理员";
        if (claims != null) {
            String realName = claims.get("realName", String.class);
            String username = claims.getSubject();
            operator = StringUtils.hasText(realName) ? realName : username;
        }

        // 2. 执行导入
        salesOrderService.importSalesData(file, operator);
        
        return Result.success("销售数据导入成功");
    }
}
