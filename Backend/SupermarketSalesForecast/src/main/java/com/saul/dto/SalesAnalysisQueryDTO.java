package com.saul.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 销售统计分析查询 DTO
 */
@Data
public class SalesAnalysisQueryDTO {

    @NotBlank(message = "开始日期不能为空")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "开始日期格式必须为 yyyy-MM-dd")
    private String startDate;

    @NotBlank(message = "结束日期不能为空")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "结束日期格式必须为 yyyy-MM-dd")
    private String endDate;
}
