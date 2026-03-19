package com.accounting.assistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "添加账单请求")
public class AddBillRequest {

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @NotNull(message = "金额不能为空")
    @Positive(message = "金额必须大于0")
    @Schema(description = "金额", example = "15.00")
    private BigDecimal amount;

    @NotNull(message = "分类ID不能为空")
    @Schema(description = "分类ID", example = "1")
    private Integer categoryId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "消费时间，默认当前时间")
    private LocalDateTime billTime;

    @Schema(description = "账单类型：1-支出，2-收入", example = "1")
    private Integer type;
}
