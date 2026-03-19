package com.accounting.assistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "更新账单请求")
public class UpdateBillRequest {

    @Schema(description = "金额")
    private BigDecimal amount;

    @Schema(description = "分类ID")
    private Integer categoryId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "消费时间")
    private LocalDateTime billTime;
}
