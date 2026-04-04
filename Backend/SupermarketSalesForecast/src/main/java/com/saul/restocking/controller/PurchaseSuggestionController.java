package com.saul.restocking.controller;

import com.saul.common.Result;
import com.saul.restocking.service.IPurchaseSuggestionService;
import com.saul.restocking.vo.PurchaseSuggestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 进货建议 Controller
 */
@RestController
@RequestMapping("/api/restocking/suggestion")
@RequiredArgsConstructor
public class PurchaseSuggestionController {

    private final IPurchaseSuggestionService suggestionService;

    /**
     * 生成进货建议
     */
    @PostMapping("/generate")
    public Result<Map<String, Object>> generateSuggestions() {
        Map<String, Object> result = suggestionService.generateSuggestions();
        return Result.success("进货建议生成成功", result);
    }

    /**
     * 查询进货建议列表
     */
    @GetMapping("/list")
    public Result<List<PurchaseSuggestionVO>> getSuggestionList(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer lightStatus,
            @RequestParam(required = false) Long categoryId) {
        List<PurchaseSuggestionVO> list = suggestionService.getSuggestionList(status, lightStatus, categoryId);
        return Result.success(list);
    }

    /**
     * 获取进货建议汇总统计
     */
    @GetMapping("/summary")
    public Result<Map<String, Object>> getSuggestionSummary() {
        Map<String, Object> summary = suggestionService.getSuggestionSummary();
        return Result.success(summary);
    }

    /**
     * 调整进货数量
     */
    @PutMapping("/adjust/{id}")
    public Result<Boolean> adjustQuantity(
            @PathVariable Long id,
            @RequestParam Integer adjustedQuantity) {
        boolean success = suggestionService.adjustQuantity(id, adjustedQuantity);
        if (success) {
            return Result.success("调整成功", true);
        }
        return Result.error("调整失败");
    }

    /**
     * 忽略进货建议
     */
    @PutMapping("/ignore/{id}")
    public Result<Boolean> ignoreSuggestion(@PathVariable Long id) {
        boolean success = suggestionService.ignoreSuggestion(id);
        if (success) {
            return Result.success("已忽略", true);
        }
        return Result.error("操作失败");
    }
}