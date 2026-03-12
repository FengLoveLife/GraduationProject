package com.saul.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 销售数据导入 Excel 映射 DTO
 */
@Data
public class SalesImportExcelDTO {

    @ExcelProperty("订单编号")
    private String orderNo;

    @ExcelProperty("商品编码")
    private String productCode;

    @ExcelProperty("实际售价")
    private BigDecimal unitPrice;

    @ExcelProperty("销售数量")
    private Integer quantity;

    @ExcelProperty("是否促销")
    private Integer isPromotion;

    @ExcelProperty("支付方式")
    private Integer paymentType;

    @ExcelProperty("销售时间")
    private String saleTime;
}
