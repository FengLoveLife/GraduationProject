package com.saul.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.saul.common.Result;
import com.saul.inventory.dto.InventoryAdjustDTO;
import com.saul.inventory.dto.InventoryLogQueryDTO;
import com.saul.inventory.service.IInventoryLogService;
import com.saul.inventory.vo.InventoryLogVO;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 库存流水 Controller
 */
@RestController
@RequestMapping("/api/inventory-log")
@RequiredArgsConstructor
public class InventoryLogController {

    private final IInventoryLogService inventoryLogService;

    /**
     * 手工盘点调整库存
     */
    @PostMapping("/adjust")
    public Result<String> adjust(@Validated @RequestBody InventoryAdjustDTO dto, HttpServletRequest request) {
        // 从拦截器存入的属性中获取 Claims
        Claims claims = (Claims) request.getAttribute("jwtClaims");
        
        String operatorName = "系统管理员 (手工调整)";
        if (claims != null) {
            String realName = claims.get("realName", String.class);
            String username = claims.getSubject();
            // 优先使用真实姓名，否则使用用户名
            operatorName = (StringUtils.hasText(realName) ? realName : username) + " (手工调整)";
        }
        
        inventoryLogService.adjustInventory(dto, operatorName);
        return Result.success("库存调整成功");
    }

    /**
     * 分页查询库存流水台账
     */
    @GetMapping("/page")
    public Result<Page<InventoryLogVO>> page(InventoryLogQueryDTO queryDTO) {
        Page<InventoryLogVO> result = inventoryLogService.queryPage(queryDTO);
        return Result.success(result);
    }

    /**
     * 导出库存流水 Excel
     */
    @GetMapping("/export")
    public void export(InventoryLogQueryDTO queryDTO, HttpServletResponse response) {
        inventoryLogService.exportLog(queryDTO, response);
    }
}
