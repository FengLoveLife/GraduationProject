package com.saul.alert.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("alert_log")
public class AlertLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 预警类型：1=库存告急  2=节假日需求预警 */
    private Integer alertType;

    /** 商品ID */
    private Long productId;

    /** 商品名称 */
    private String productName;

    /** 通知正文 */
    private String alertContent;

    /** 是否已读：0=未读  1=已读 */
    private Integer isRead;

    /** 业务日期（去重用，同类型同商品同天只生成一条） */
    private LocalDate bizDate;

    /** 创建时间 */
    private LocalDateTime createTime;
}
