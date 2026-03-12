package com.saul.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 库存变动流水实体类
 */
@Data
@TableName("inventory_log")
public class InventoryLog implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 流水单号 */
    private String logNo;

    /** 关联商品ID */
    private Long productId;

    /** 变动类型：1进货入库, 2销售出库, 3损耗盘亏, 4手工调整 */
    private Integer type;

    /** 变动数量 */
    private Integer changeAmount;

    /** 变动前库存 */
    private Integer beforeStock;

    /** 变动后库存 */
    private Integer afterStock;

    /** 关联业务单号 */
    private String referenceNo;

    /** 操作人 */
    private String operator;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
