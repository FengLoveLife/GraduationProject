package com.saul.restocking.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.saul.restocking.entity.PurchaseSuggestion;
import com.saul.restocking.vo.PurchaseSuggestionVO;

import java.util.List;
import java.util.Map;

/**
 * 进货建议服务接口
 */
public interface IPurchaseSuggestionService extends IService<PurchaseSuggestion> {

    /**
     * 生成进货建议（核心方法）
     * 执行红黄绿灯判断逻辑，生成进货建议记录
     * @return 生成结果统计
     */
    Map<String, Object> generateSuggestions();

    /**
     * 查询进货建议列表
     * @param status 状态筛选（可选）
     * @param lightStatus 灯位筛选（可选）
     * @param categoryId 分类筛选（可选）
     * @return 建议列表
     */
    List<PurchaseSuggestionVO> getSuggestionList(Integer status, Integer lightStatus, Long categoryId);

    /**
     * 获取进货建议汇总统计
     * @return 统计数据
     */
    Map<String, Object> getSuggestionSummary();

    /**
     * 调整进货数量
     * @param id 建议ID
     * @param adjustedQuantity 调整后的数量
     * @return 是否成功
     */
    boolean adjustQuantity(Long id, Integer adjustedQuantity);

    /**
     * 忽略进货建议
     * @param id 建议ID
     * @return 是否成功
     */
    boolean ignoreSuggestion(Long id);
}