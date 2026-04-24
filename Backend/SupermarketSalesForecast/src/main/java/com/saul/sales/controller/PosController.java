package com.saul.sales.controller;

import com.saul.common.Result;
import com.saul.common.annotation.OperationLog;
import com.saul.sales.dto.PosOrderDTO;
import com.saul.sales.service.IPosOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * POS/收银系统对接控制器
 * <p>
 * 提供两组接口：
 * <ol>
 *   <li><b>业务接口</b>：对外暴露给 POS 机的数据推送入口（带 Token 鉴权）</li>
 *   <li><b>管理接口</b>：供前端"POS 接入管理"页面调用，用于监控与联调</li>
 * </ol>
 * <p>
 * 设计原则：接口 RESTful + JSON，兼容主流收银系统；与 Excel 导入共用同一套落库逻辑，
 * 保证"销售数据入口多样化"但"业务语义一致"。
 */
@RestController
@RequestMapping("/api/pos")
@RequiredArgsConstructor
public class PosController {

    private final IPosOrderService posOrderService;

    /** POS 推送密钥（配置在 application.yml，真实场景一机一密） */
    @Value("${pos.secret:POS-SECRET-2026}")
    private String posSecret;

    /** POS 对接启用开关（可快速禁用所有业务接口） */
    @Value("${pos.enabled:true}")
    private boolean posEnabled;

    private static final DateTimeFormatter FULL_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ====================================================================
    // 一、业务接口（暴露给 POS/收银系统）
    // ====================================================================

    /**
     * 【业务接口】单笔销售记录推送
     * <p>每笔收银完成后 POS 立即调用
     */
    @PostMapping("/push-order")
    @OperationLog(type = "POS", desc = "POS 单笔推送")
    public Result<Map<String, Object>> pushOrder(
            @RequestHeader(value = "X-POS-Token", required = false) String token,
            @RequestBody PosOrderDTO dto) {
        Result<Map<String, Object>> authErr = checkAuth(token);
        if (authErr != null) return authErr;

        Map<String, Object> r = posOrderService.receivePosOrder(dto);
        return Result.success("销售记录接收成功", r);
    }

    /**
     * 【业务接口】批量销售记录推送（断网补传）
     */
    @PostMapping("/push-orders-batch")
    @OperationLog(type = "POS", desc = "POS 批量推送")
    public Result<Map<String, Object>> pushOrdersBatch(
            @RequestHeader(value = "X-POS-Token", required = false) String token,
            @RequestBody List<PosOrderDTO> orders) {
        Result<Map<String, Object>> authErr = checkAuth(token);
        if (authErr != null) return authErr;

        Map<String, Object> r = posOrderService.receivePosOrderBatch(orders);
        return Result.success("批量接收完成", r);
    }

    /**
     * 【业务接口】心跳检测（POS 机可定期调用）
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> m = new HashMap<>();
        m.put("status", posEnabled ? "UP" : "DISABLED");
        m.put("serverTime", LocalDateTime.now().format(FULL_FMT));
        m.put("protocol", "REST + JSON");
        m.put("version", "v1.0");
        return Result.success(m);
    }

    // ====================================================================
    // 二、管理接口（供前端"POS 接入管理"页面调用）
    // ====================================================================

    /**
     * 【管理】接入配置信息（URL / Token / 协议说明）
     */
    @GetMapping("/config")
    public Result<Map<String, Object>> getConfig() {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("pushUrl", "/api/pos/push-order");
        cfg.put("batchUrl", "/api/pos/push-orders-batch");
        cfg.put("healthUrl", "/api/pos/health");
        cfg.put("tokenHeader", "X-POS-Token");
        cfg.put("token", posSecret);
        cfg.put("protocol", "REST + JSON");
        cfg.put("version", "v1.0");
        cfg.put("enabled", posEnabled);
        return Result.success(cfg);
    }

    /**
     * 【管理】接入终端列表（聚合历史 POS 订单得出所有活跃的 POS 机）
     */
    @GetMapping("/terminals")
    public Result<List<Map<String, Object>>> getTerminals() {
        return Result.success(posOrderService.listPosTerminals());
    }

    /**
     * 【管理】最近 POS 推送流水
     */
    @GetMapping("/recent-orders")
    public Result<List<Map<String, Object>>> getRecentOrders(
            @RequestParam(defaultValue = "20") Integer limit) {
        return Result.success(posOrderService.recentPosOrders(limit));
    }

    /**
     * 【管理】推送统计（今日 / 本月 / 累计 / 终端数）
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        return Result.success(posOrderService.posStatistics());
    }

    /**
     * 【管理】模拟 POS 推送（答辩演示核心工具）
     * <p>
     * 服务端自调用 receivePosOrder，复用完整的落库链路，
     * 相当于把"真实 POS"的推送流程在本地演绎一次。
     */
    @PostMapping("/simulate-push")
    @OperationLog(type = "POS", desc = "模拟 POS 推送")
    public Result<Map<String, Object>> simulatePush(@RequestBody PosOrderDTO dto) {
        // 若前端没指定订单号，自动生成带 POS 前缀的订单号（保持可识别性）
        if (dto.getOrderNo() == null || dto.getOrderNo().isBlank()) {
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
            dto.setOrderNo("POS" + ts);
        }
        if (dto.getSaleTime() == null || dto.getSaleTime().isBlank()) {
            dto.setSaleTime(LocalDateTime.now().format(FULL_FMT));
        }
        if (dto.getSaleDate() == null || dto.getSaleDate().isBlank()) {
            dto.setSaleDate(LocalDateTime.now().toLocalDate().toString());
        }
        Map<String, Object> r = posOrderService.receivePosOrder(dto);
        return Result.success("模拟推送成功", r);
    }

    // ====================================================================
    // 内部工具
    // ====================================================================

    private Result<Map<String, Object>> checkAuth(String token) {
        if (!posEnabled) {
            return Result.error(503, "POS 对接已禁用");
        }
        if (token == null || !token.equals(posSecret)) {
            return Result.error(401, "POS Token 无效，拒绝接收");
        }
        return null;
    }
}
