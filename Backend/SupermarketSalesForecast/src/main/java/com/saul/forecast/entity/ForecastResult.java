package com.saul.forecast.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 销量预测结果实体
 */
@Data
@TableName("forecast_result")
public class ForecastResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 预测的目标日期 */
    private LocalDate forecastDate;

    /** 商品ID */
    private Long productId;

    /** 商品编码(冗余) */
    private String productCode;

    /** 商品名称(冗余) */
    private String productName;

    /** 分类ID(冗余) */
    private Long categoryId;

    /** 分类名称(冗余) */
    private String categoryName;

    /** 预测销量 */
    private Integer predictedQuantity;



    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}