package com.saul.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体类
 */
@Data
@TableName("product")
public class Product implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联分类表ID
     */
    @TableField("category_id")
    private Long categoryId;

    /**
     * 商品系统编码（唯一）
     */
    @TableField("product_code")
    private String productCode;

    /**
     * 商品名称
     */
    @TableField("name")
    private String name;

    /**
     * 规格
     */
    @TableField("specification")
    private String specification;

    /**
     * 计价单位
     */
    @TableField("unit")
    private String unit;

    /**
     * 图片地址
     */
    @TableField("image_url")
    private String imageUrl;

    /**
     * 进货价
     */
    @TableField("purchase_price")
    private BigDecimal purchasePrice;

    /**
     * 零售价
     */
    @TableField("sale_price")
    private BigDecimal salePrice;

    /**
     * 当前库存量
     */
    @TableField("stock")
    private Integer stock;

    /**
     * 安全库存阈值
     */
    @TableField("safety_stock")
    private Integer safetyStock;

    /**
     * 状态: 1-上架, 0-下架
     */
    @TableField("status")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
