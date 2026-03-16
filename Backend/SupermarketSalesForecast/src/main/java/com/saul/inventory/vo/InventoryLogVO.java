package com.saul.inventory.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存流水展示 VO
 */
@Data
public class InventoryLogVO {
    private Long id;
    private String logNo;
    private Long productId;
    private Integer type;
    private Integer changeAmount;
    private Integer beforeStock;
    private Integer afterStock;
    private String referenceNo;
    private String operator;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updateTime;

    // --- 额外增加的商品展示字段 ---
    /** 商品编码 */
    private String productCode;
    /** 商品名称 */
    private String productName;
    /** 商品图片 */
    private String imageUrl;
}
