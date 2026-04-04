package com.saul.forecast.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.saul.forecast.entity.ForecastResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 销量预测结果 Mapper
 */
@Mapper
public interface ForecastResultMapper extends BaseMapper<ForecastResult> {
}